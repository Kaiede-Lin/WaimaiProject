package com.waimai.service.impl;

import com.waimai.service.push.OrderPushService;
import com.waimai.common.constant.OrderStatus;
import com.waimai.common.entity.Order;
import com.waimai.common.entity.Payment;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.mapper.PaymentMapper;
import com.waimai.service.service.PaymentService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final String PAY_IDEMPOTENT_KEY = "waimai:pay:";

    private final OrderServiceImpl orderService;
    private final PaymentMapper paymentMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderPushService orderPushService;

    public PaymentServiceImpl(OrderServiceImpl orderService, PaymentMapper paymentMapper,
                              RedisTemplate<String, Object> redisTemplate,
                              OrderPushService orderPushService) {
        this.orderService = orderService;
        this.paymentMapper = paymentMapper;
        this.redisTemplate = redisTemplate;
        this.orderPushService = orderPushService;
    }

    @Override
    @Transactional
    public String mockPay(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // Idempotency guard: Redis SETNX on order_id, 5min TTL
        String idempotentKey = PAY_IDEMPOTENT_KEY + orderId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            // Already being processed — return existing payment
            Payment existing = paymentMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payment>()
                            .eq(Payment::getOrderId, orderId)
                            .eq(Payment::getStatus, "SUCCESS"));
            if (existing != null) {
                return existing.getPayNo();
            }
            throw new BusinessException("支付正在处理中，请稍后");
        }

        // State machine guard: only PENDING_PAYMENT can be paid
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException("订单状态异常，无法支付。当前状态: " + order.getStatus());
        }

        // Generate pay number
        String payNo = "PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();

        // Create payment record
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPayNo(payNo);
        payment.setAmount(order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO);
        payment.setMethod("MOCK");
        payment.setStatus("SUCCESS");
        payment.setPayTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        // Update order status
        orderService.payOrder(orderId);

        // Push new order notification to merchant via WebSocket
        orderPushService.pushNewOrderToMerchant(orderId);

        return payNo;
    }

    @Override
    public boolean isPayProcessed(String payNo) {
        return paymentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPayNo, payNo)
                        .eq(Payment::getStatus, "SUCCESS")) != null;
    }
}
