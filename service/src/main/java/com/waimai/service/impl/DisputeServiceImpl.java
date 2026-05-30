package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waimai.common.constant.DeliveryExceptionStatus;
import com.waimai.common.constant.DisputeStatus;
import com.waimai.common.constant.OrderStatus;
import com.waimai.common.dto.CreateDisputeDTO;
import com.waimai.common.dto.ReportExceptionDTO;
import com.waimai.common.dto.RequestRefundDTO;
import com.waimai.common.entity.*;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.mapper.DeliveryExceptionMapper;
import com.waimai.service.mapper.OrderDisputeMapper;
import com.waimai.service.mapper.OrderMapper;
import com.waimai.service.mapper.PaymentMapper;
import com.waimai.service.service.DisputeService;
import com.waimai.service.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class DisputeServiceImpl implements DisputeService {

    private final OrderDisputeMapper disputeMapper;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final DeliveryExceptionMapper deliveryExceptionMapper;
    private final OrderService orderService;

    private static final Set<String> REFUNDABLE_STATUSES = Set.of(
            OrderStatus.PAID, OrderStatus.PREPARING,
            OrderStatus.ACCEPTED, OrderStatus.DELIVERING, OrderStatus.COMPLETED);

    public DisputeServiceImpl(OrderDisputeMapper disputeMapper, OrderMapper orderMapper,
                              PaymentMapper paymentMapper, DeliveryExceptionMapper deliveryExceptionMapper,
                              OrderService orderService) {
        this.disputeMapper = disputeMapper;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.deliveryExceptionMapper = deliveryExceptionMapper;
        this.orderService = orderService;
    }

    @Override
    @Transactional
    public OrderDispute createDispute(Long userId, CreateDisputeDTO dto) {
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new BusinessException("只能对已完成的订单发起纠纷");
        }

        Long count = disputeMapper.selectCount(new LambdaQueryWrapper<OrderDispute>()
                .eq(OrderDispute::getOrderId, dto.getOrderId())
                .eq(OrderDispute::getUserId, userId));
        if (count > 0) throw new BusinessException("已对该订单发起过纠纷");

        OrderDispute dispute = new OrderDispute();
        dispute.setOrderId(dto.getOrderId());
        dispute.setUserId(userId);
        dispute.setType(dto.getType());
        dispute.setDescription(dto.getDescription());
        dispute.setImages(dto.getImages());
        dispute.setStatus(DisputeStatus.PENDING);
        disputeMapper.insert(dispute);
        return dispute;
    }

    @Override
    public List<OrderDispute> listByUser(Long userId) {
        return disputeMapper.selectList(new LambdaQueryWrapper<OrderDispute>()
                .eq(OrderDispute::getUserId, userId)
                .orderByDesc(OrderDispute::getCreateTime));
    }

    @Override
    public List<OrderDispute> listAll(String status) {
        LambdaQueryWrapper<OrderDispute> qw = new LambdaQueryWrapper<OrderDispute>()
                .orderByDesc(OrderDispute::getCreateTime);
        if (status != null && !status.isEmpty()) {
            qw.eq(OrderDispute::getStatus, status);
        }
        return disputeMapper.selectList(qw);
    }

    @Override
    @Transactional
    public void resolve(Long disputeId, String status, String remark, String resolution) {
        OrderDispute dispute = disputeMapper.selectById(disputeId);
        if (dispute == null) throw new BusinessException("纠纷不存在");
        dispute.setStatus(status);
        dispute.setAdminRemark(remark);
        dispute.setResolution(resolution);
        disputeMapper.updateById(dispute);
    }

    // ── Refund ─────────────────────────────────────────────

    @Override
    @Transactional
    public OrderDispute requestRefund(Long userId, RequestRefundDTO dto) {
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (!REFUNDABLE_STATUSES.contains(order.getStatus())) {
            throw new BusinessException("当前订单状态不支持退款");
        }

        // Check duplicate
        Long count = disputeMapper.selectCount(new LambdaQueryWrapper<OrderDispute>()
                .eq(OrderDispute::getOrderId, dto.getOrderId())
                .eq(OrderDispute::getUserId, userId));
        if (count > 0) throw new BusinessException("已对该订单发起过纠纷或退款");

        OrderDispute dispute = new OrderDispute();
        dispute.setOrderId(dto.getOrderId());
        dispute.setUserId(userId);
        dispute.setType(dto.getType());
        dispute.setDescription(dto.getDescription());
        dispute.setImages(dto.getImages());
        dispute.setStatus(DisputeStatus.PENDING);
        dispute.setRefundStatus("REQUESTED");
        dispute.setPreviousStatus(order.getStatus());

        // Transition order to REFUNDING
        order.setStatus(OrderStatus.REFUNDING);
        orderMapper.updateById(order);

        disputeMapper.insert(dispute);
        return dispute;
    }

    @Override
    @Transactional
    public void cancelRefund(Long userId, Long disputeId) {
        OrderDispute dispute = disputeMapper.selectById(disputeId);
        if (dispute == null || !dispute.getUserId().equals(userId)) {
            throw new BusinessException("纠纷不存在");
        }
        if (!"REQUESTED".equals(dispute.getRefundStatus())) {
            throw new BusinessException("退款申请已被处理，无法取消");
        }

        // Restore order status
        Order order = orderMapper.selectById(dispute.getOrderId());
        if (order != null && OrderStatus.REFUNDING.equals(order.getStatus())) {
            order.setStatus(dispute.getPreviousStatus() != null
                    ? dispute.getPreviousStatus() : OrderStatus.PAID);
            orderMapper.updateById(order);
        }

        dispute.setStatus(DisputeStatus.REJECTED);
        dispute.setRefundStatus("REJECTED");
        dispute.setResolution("用户取消");
        disputeMapper.updateById(dispute);
    }

    // ── Merchant ──────────────────────────────────────────

    @Override
    public List<OrderDispute> listByMerchant(Long merchantId, String refundStatus) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId));
        if (orders.isEmpty()) return List.of();

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        LambdaQueryWrapper<OrderDispute> qw = new LambdaQueryWrapper<OrderDispute>()
                .in(OrderDispute::getOrderId, orderIds)
                .orderByDesc(OrderDispute::getCreateTime);
        if (refundStatus != null && !refundStatus.isEmpty()) {
            qw.eq(OrderDispute::getRefundStatus, refundStatus);
        }
        return disputeMapper.selectList(qw);
    }

    @Override
    @Transactional
    public void handleByMerchant(Long merchantId, Long disputeId, boolean approved, String remark) {
        OrderDispute dispute = disputeMapper.selectById(disputeId);
        if (dispute == null) throw new BusinessException("纠纷不存在");

        Order order = orderMapper.selectById(dispute.getOrderId());
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权处理该纠纷");
        }
        if (!"REQUESTED".equals(dispute.getRefundStatus())) {
            throw new BusinessException("退款申请已处理过");
        }
        if (!OrderStatus.REFUNDING.equals(order.getStatus())) {
            throw new BusinessException("订单状态异常");
        }

        dispute.setMerchantRemark(remark);

        if (approved) {
            dispute.setRefundStatus("APPROVED");
            dispute.setStatus(DisputeStatus.RESOLVED);
            dispute.setResolution("商户同意退款");

            // Update order to REFUNDED
            order.setStatus(OrderStatus.REFUNDED);
            orderMapper.updateById(order);

            // Rollback inventory
            orderService.rollbackInventory(order.getId());

            // Mark payment as REFUNDED
            Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                    .eq(Payment::getOrderId, order.getId()));
            if (payment != null) {
                payment.setStatus("REFUNDED");
                paymentMapper.updateById(payment);
            }
        } else {
            dispute.setRefundStatus("REJECTED");
            dispute.setStatus(DisputeStatus.REJECTED);
            dispute.setResolution("商户拒绝退款");

            // Restore order to previous status
            order.setStatus(dispute.getPreviousStatus() != null
                    ? dispute.getPreviousStatus() : OrderStatus.PAID);
            orderMapper.updateById(order);
        }

        disputeMapper.updateById(dispute);
    }

    // ── Rider exception ───────────────────────────────────

    @Override
    @Transactional
    public DeliveryException reportException(Long riderId, ReportExceptionDTO dto) {
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || (order.getRiderId() != null && !order.getRiderId().equals(riderId))) {
            throw new BusinessException("订单不存在或您无权处理该订单");
        }

        DeliveryException ex = new DeliveryException();
        ex.setOrderId(dto.getOrderId());
        ex.setRiderId(riderId);
        ex.setType(dto.getType());
        ex.setDescription(dto.getDescription());
        ex.setImages(dto.getImages());
        ex.setStatus(DeliveryExceptionStatus.REPORTED);
        deliveryExceptionMapper.insert(ex);
        return ex;
    }

    @Override
    public List<DeliveryException> listExceptionsByRider(Long riderId) {
        return deliveryExceptionMapper.selectList(new LambdaQueryWrapper<DeliveryException>()
                .eq(DeliveryException::getRiderId, riderId)
                .orderByDesc(DeliveryException::getCreateTime));
    }
}
