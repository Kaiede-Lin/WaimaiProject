package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waimai.common.constant.OrderStatus;
import com.waimai.common.entity.Order;
import com.waimai.common.entity.OrderDetail;
import com.waimai.common.vo.MerchantReportVO;
import com.waimai.service.mapper.OrderDetailMapper;
import com.waimai.service.mapper.OrderMapper;
import com.waimai.service.service.ReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;

    public ReportServiceImpl(OrderMapper orderMapper, OrderDetailMapper orderDetailMapper) {
        this.orderMapper = orderMapper;
        this.orderDetailMapper = orderDetailMapper;
    }

    @Override
    public MerchantReportVO dailyReport(Long merchantId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return buildReport(merchantId, start, end);
    }

    @Override
    public MerchantReportVO weeklyReport(Long merchantId, LocalDate startDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = startDate.plusDays(7).atStartOfDay();
        return buildReport(merchantId, start, end);
    }

    @Override
    public MerchantReportVO monthlyReport(Long merchantId, String month) {
        int year = Integer.parseInt(month.substring(0, 4));
        int m = Integer.parseInt(month.substring(5, 7));
        LocalDateTime start = LocalDate.of(year, m, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        return buildReport(merchantId, start, end);
    }

    private MerchantReportVO buildReport(Long merchantId, LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .ne(Order::getStatus, OrderStatus.CANCELLED)
                .ge(Order::getCreateTime, start)
                .lt(Order::getCreateTime, end));

        BigDecimal totalRevenue = orders.stream()
                .map(o -> o.getPayAmount() != null ? o.getPayAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long orderCount = orders.size();
        BigDecimal avgOrderValue = orderCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Top dishes
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<Map<String, Object>> topDishes = new ArrayList<>();
        if (!orderIds.isEmpty()) {
            List<OrderDetail> details = orderDetailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().in(OrderDetail::getOrderId, orderIds));
            Map<String, Long> dishCounts = details.stream()
                    .collect(Collectors.groupingBy(OrderDetail::getDishName, Collectors.counting()));
            topDishes = dishCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", e.getKey());
                        m.put("count", e.getValue());
                        return m;
                    }).collect(Collectors.toList());
        }

        MerchantReportVO vo = new MerchantReportVO();
        vo.setTotalRevenue(totalRevenue);
        vo.setOrderCount(orderCount);
        vo.setAvgOrderValue(avgOrderValue);
        vo.setTopDishes(topDishes);
        return vo;
    }
}
