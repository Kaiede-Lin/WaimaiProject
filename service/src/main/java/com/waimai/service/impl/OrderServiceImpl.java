package com.waimai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waimai.service.config.RabbitMQConfig;
import com.waimai.common.constant.OrderStatus;
import com.waimai.common.dto.OrderItemDTO;
import com.waimai.common.entity.*;
import com.waimai.common.exception.BusinessException;
import com.waimai.common.utils.SnowflakeUtil;
import com.waimai.service.mapper.OrderDetailMapper;
import com.waimai.service.mapper.OrderMapper;
import com.waimai.service.service.OrderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderDetailMapper orderDetailMapper;
    private final DishServiceImpl dishService;
    private final MerchantServiceImpl merchantService;
    private final RedisInventoryServiceImpl redisInventoryService;
    private final CartServiceImpl cartService;
    private final SnowflakeUtil snowflakeUtil;
    private final RabbitTemplate rabbitTemplate;
    private final CouponServiceImpl couponService;

    public OrderServiceImpl(OrderDetailMapper orderDetailMapper, DishServiceImpl dishService,
                            MerchantServiceImpl merchantService,
                            RedisInventoryServiceImpl redisInventoryService,
                            CartServiceImpl cartService,
                            SnowflakeUtil snowflakeUtil,
                            RabbitTemplate rabbitTemplate,
                            CouponServiceImpl couponService) {
        this.orderDetailMapper = orderDetailMapper;
        this.dishService = dishService;
        this.merchantService = merchantService;
        this.redisInventoryService = redisInventoryService;
        this.cartService = cartService;
        this.snowflakeUtil = snowflakeUtil;
        this.rabbitTemplate = rabbitTemplate;
        this.couponService = couponService;
    }

    @Override
    @Transactional
    public Order placeOrder(Long userId, Long merchantId, String address,
                            double addressLng, double addressLat, String remark,
                            List<OrderItemDTO> items, Long couponId) {
        // 1. Validate merchant
        Merchant merchant = merchantService.getById(merchantId);
        if (merchant == null || merchant.getStatus() != 1) {
            throw new BusinessException("商家不存在或已停业");
        }

        // 2. Validate dishes exist and are on sale, compute total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Long> dishIds = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        for (OrderItemDTO item : items) {
            Dish dish = dishService.getById(item.getDishId());
            if (dish == null || dish.getStatus() != 1) {
                throw new BusinessException("菜品【" + item.getDishName() + "】已下架");
            }
            item.setDishName(dish.getName());
            item.setDishImage(dish.getImage());
            item.setPrice(dish.getPrice());
            totalAmount = totalAmount.add(dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

            dishIds.add(item.getDishId());
            quantities.add(item.getQuantity());
        }

        // 3. Atomic inventory deduction via Redis Lua script
        redisInventoryService.deductInventory(merchantId, dishIds, quantities);

        // 3.5 Apply coupon if provided
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (couponId != null) {
            discountAmount = couponService.applyCoupon(couponId, userId, totalAmount);
        }

        // 4. Generate snowflake order number
        String orderNo = String.valueOf(snowflakeUtil.nextId());

        // 5. Insert order into MySQL (status = PENDING_PAYMENT)
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(totalAmount);
        BigDecimal deliveryFee = merchant.getDeliveryFee();
        if (deliveryFee == null || deliveryFee.compareTo(BigDecimal.ZERO) <= 0) {
            deliveryFee = new BigDecimal("5.00");
        }
        order.setDeliveryFee(deliveryFee);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(totalAmount.add(order.getDeliveryFee()).subtract(discountAmount));
        order.setAddress(address);
        order.setAddressLng(BigDecimal.valueOf(addressLng));
        order.setAddressLat(BigDecimal.valueOf(addressLat));
        order.setRemark(remark);
        save(order);

        // 6. Insert order details (price snapshot)
        for (OrderItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(order.getId());
            detail.setDishId(item.getDishId());
            detail.setDishName(item.getDishName());
            detail.setDishImage(item.getDishImage());
            detail.setPrice(item.getPrice());
            detail.setQuantity(item.getQuantity());
            orderDetailMapper.insert(detail);
        }

        // 7. Send delay message to RabbitMQ (30min TTL → DLX → timeout cancel)
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_DELAY_EXCHANGE,
                RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                orderNo);

        // 8. Decrement MySQL dish stock (async with Redis; eventually consistent)
        for (OrderItemDTO item : items) {
            Dish dish = dishService.getById(item.getDishId());
            if (dish != null) {
                dish.setStock(dish.getStock() - item.getQuantity());
                dish.setMonthlySales(dish.getMonthlySales() + item.getQuantity());
                dishService.updateById(dish);
            }
        }

        // 9. Clear user's cart
        cartService.clearCart(userId);

        return order;
    }

    @Override
    @Transactional
    public Order payOrder(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException("订单状态异常，当前状态: " + order.getStatus());
        }
        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        updateById(order);

        // Update merchant monthly sales
        Merchant merchant = merchantService.getById(order.getMerchantId());
        if (merchant != null) {
            merchant.setMonthlySales(merchant.getMonthlySales() + 1);
            merchantService.updateById(merchant);
        }

        return order;
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许取消，当前状态: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        updateById(order);

        // Rollback inventory
        rollbackOrderInventory(order);
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return lambdaQuery().eq(Order::getOrderNo, orderNo).one();
    }

    @Override
    public Page<Order> listByUser(Long userId, int page, int size) {
        return lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .page(new Page<>(page, size));
    }

    @Override
    public Page<Order> listByMerchant(Long merchantId, int page, int size) {
        return lambdaQuery()
                .eq(Order::getMerchantId, merchantId)
                .orderByDesc(Order::getCreateTime)
                .page(new Page<>(page, size));
    }

    @Override
    @Transactional
    public void acceptOrder(Long orderId, Long merchantId) {
        Order order = getById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new BusinessException("订单状态异常，当前状态: " + order.getStatus());
        }
        order.setStatus(OrderStatus.PREPARING);
        updateById(order);
    }

    /**
     * Timeout cancel — called by DLX consumer.
     * Idempotent: only acts if status is still PENDING_PAYMENT.
     */
    @Transactional
    public void timeoutCancel(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) return;
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) return;

        order.setStatus(OrderStatus.CANCELLED);
        updateById(order);
        rollbackOrderInventory(order);
    }

    private void rollbackOrderInventory(Order order) {
        List<OrderDetail> details = orderDetailMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderDetail>()
                        .eq(OrderDetail::getOrderId, order.getId()));

        if (details.isEmpty()) return;

        List<Long> dishIds = details.stream().map(OrderDetail::getDishId).collect(Collectors.toList());
        List<Integer> quantities = details.stream().map(OrderDetail::getQuantity).collect(Collectors.toList());

        redisInventoryService.rollbackInventory(order.getMerchantId(), dishIds, quantities);
    }
}
