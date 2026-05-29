package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waimai.common.constant.OrderStatus;
import com.waimai.common.entity.Order;
import com.waimai.service.mapper.OrderMapper;
import com.waimai.service.service.OvertimeWarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OvertimeWarningServiceImpl implements OvertimeWarningService {

    private final OrderMapper orderMapper;

    public OvertimeWarningServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    @Scheduled(fixedDelay = 120000)
    public void checkOvertimeOrders() {
        List<Order> deliveringOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.DELIVERING)
                        .isNotNull(Order::getEstimatedMinutes));

        LocalDateTime now = LocalDateTime.now();
        for (Order order : deliveringOrders) {
            if (order.getDeliverTime() == null || order.getEstimatedMinutes() == null) continue;

            int bufferMinutes = 10;
            LocalDateTime deadline = order.getDeliverTime().plusMinutes(order.getEstimatedMinutes() + bufferMinutes);

            if (now.isAfter(deadline)) {
                if (order.getIsOvertime() == null || order.getIsOvertime() == 0) {
                    order.setIsOvertime(1);
                    orderMapper.updateById(order);
                    log.warn("订单超时: orderNo={}, riderId={}, estimatedMinutes={}",
                            order.getOrderNo(), order.getRiderId(), order.getEstimatedMinutes());
                }
            }
        }
    }
}
