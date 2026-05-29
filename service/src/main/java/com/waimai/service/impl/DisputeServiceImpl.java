package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waimai.common.constant.DisputeStatus;
import com.waimai.common.constant.OrderStatus;
import com.waimai.common.dto.CreateDisputeDTO;
import com.waimai.common.entity.Order;
import com.waimai.common.entity.OrderDispute;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.mapper.OrderDisputeMapper;
import com.waimai.service.mapper.OrderMapper;
import com.waimai.service.service.DisputeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DisputeServiceImpl implements DisputeService {

    private final OrderDisputeMapper disputeMapper;
    private final OrderMapper orderMapper;

    public DisputeServiceImpl(OrderDisputeMapper disputeMapper, OrderMapper orderMapper) {
        this.disputeMapper = disputeMapper;
        this.orderMapper = orderMapper;
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

        // Check duplicate
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
}
