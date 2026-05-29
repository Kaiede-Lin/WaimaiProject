package com.waimai.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waimai.common.Result;
import com.waimai.common.constant.OrderStatus;
import com.waimai.common.entity.Merchant;
import com.waimai.common.entity.Order;
import com.waimai.common.entity.Rider;
import com.waimai.common.entity.User;
import com.waimai.service.mapper.CategoryMapper;
import com.waimai.service.mapper.DishMapper;
import com.waimai.service.mapper.MerchantMapper;
import com.waimai.service.mapper.OrderMapper;
import com.waimai.service.mapper.RiderMapper;
import com.waimai.service.mapper.UserMapper;
import com.waimai.service.service.MerchantService;
import com.waimai.service.service.RiderService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final RiderMapper riderMapper;
    private final DishMapper dishMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantService merchantService;
    private final RiderService riderService;

    public AdminController(OrderMapper orderMapper, MerchantMapper merchantMapper,
                           UserMapper userMapper, RiderMapper riderMapper,
                           DishMapper dishMapper, CategoryMapper categoryMapper,
                           MerchantService merchantService, RiderService riderService) {
        this.orderMapper = orderMapper;
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
        this.riderMapper = riderMapper;
        this.dishMapper = dishMapper;
        this.categoryMapper = categoryMapper;
        this.merchantService = merchantService;
        this.riderService = riderService;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();

        // Today stats
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Order> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart));

        long todayOrderCount = todayOrders.size();
        double todayRevenue = todayOrders.stream()
                .filter(o -> !OrderStatus.CANCELLED.equals(o.getStatus()))
                .mapToDouble(o -> o.getPayAmount() != null ? o.getPayAmount().doubleValue() : 0)
                .sum();

        // Total counts
        long totalMerchants = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getStatus, 1));
        long totalUsers = userMapper.selectCount(null);
        long onlineRiders = riderMapper.selectCount(new LambdaQueryWrapper<Rider>()
                .eq(Rider::getStatus, 3));

        // Last 7 days order counts
        List<Map<String, Object>> last7Days = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, dayStart)
                    .lt(Order::getCreateTime, dayEnd));
            Map<String, Object> day = new HashMap<>();
            day.put("date", date.format(fmt));
            day.put("count", count);
            last7Days.add(day);
        }

        data.put("todayOrders", todayOrderCount);
        data.put("todayRevenue", todayRevenue);
        data.put("totalMerchants", totalMerchants);
        data.put("totalUsers", totalUsers);
        data.put("onlineRiders", onlineRiders);
        data.put("last7Days", last7Days);

        return Result.ok(data);
    }

    @GetMapping("/merchant/audit")
    public Result<List<Merchant>> auditList(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Merchant> qw = new LambdaQueryWrapper<Merchant>()
                .orderByAsc(Merchant::getCreateTime);
        if (status != null) {
            qw.eq(Merchant::getStatus, status);
        }
        return Result.ok(merchantMapper.selectList(qw));
    }

    @PostMapping("/merchant/{id}/audit")
    public Result<?> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = (Integer) body.get("status");
        String reason = (String) body.getOrDefault("reason", "");
        merchantService.audit(id, status, reason);
        return Result.ok();
    }

    @GetMapping("/order/monitor")
    public Result<Map<String, Object>> orderMonitor(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreateTime);
        if (status != null && !status.isEmpty()) {
            qw.eq(Order::getStatus, status);
        }

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), qw);

        // Enrich with names
        List<Map<String, Object>> enriched = orderPage.getRecords().stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("merchantId", o.getMerchantId());
            m.put("userId", o.getUserId());
            m.put("riderId", o.getRiderId());
            m.put("status", o.getStatus());
            m.put("totalAmount", o.getTotalAmount());
            m.put("payAmount", o.getPayAmount());
            m.put("address", o.getAddress());
            m.put("createTime", o.getCreateTime());

            Merchant merchant = merchantMapper.selectById(o.getMerchantId());
            m.put("merchantName", merchant != null ? merchant.getName() : "未知");

            User user = userMapper.selectById(o.getUserId());
            m.put("userName", user != null ? user.getNickname() : "未知");

            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", enriched);
        result.put("total", orderPage.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.ok(result);
    }

    @GetMapping("/rider/audit")
    public Result<List<Map<String, Object>>> riderAuditList(@RequestParam(required = false) Integer auditStatus) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Rider> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Rider>()
                        .orderByAsc(Rider::getCreateTime);
        if (auditStatus != null) {
            qw.eq(Rider::getAuditStatus, auditStatus);
        }
        List<Rider> riders = riderMapper.selectList(qw);
        List<Map<String, Object>> list = riders.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("openid", r.getOpenid());
            m.put("realName", r.getRealName());
            m.put("idCard", r.getIdCard());
            m.put("phone", r.getPhone());
            m.put("avatar", r.getAvatar());
            m.put("auditStatus", r.getAuditStatus());
            m.put("rejectionReason", r.getRejectionReason());
            m.put("status", r.getStatus());
            m.put("totalOrders", r.getTotalOrders());
            m.put("score", r.getScore() != null ? r.getScore() : BigDecimal.valueOf(5.0));
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(list);
    }

    @PostMapping("/rider/{id}/audit")
    public Result<?> auditRider(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer auditStatus = (Integer) body.get("auditStatus");
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            return Result.fail(400, "auditStatus 必须为 1(通过) 或 2(驳回)");
        }
        String reason = (String) body.getOrDefault("reason", "");
        riderService.auditRider(id, auditStatus, reason);
        return Result.ok();
    }

    @GetMapping("/overtime/list")
    public Result<List<Map<String, Object>>> overtimeList() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Order> list = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getIsOvertime, 1)
                        .orderByDesc(Order::getCreateTime));

        List<Map<String, Object>> result = list.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("status", o.getStatus());
            m.put("estimatedMinutes", o.getEstimatedMinutes());
            m.put("deliverTime", o.getDeliverTime());

            Merchant merchant = merchantMapper.selectById(o.getMerchantId());
            m.put("merchantName", merchant != null ? merchant.getName() : "未知");

            Rider rider = o.getRiderId() != null ? riderMapper.selectById(o.getRiderId()) : null;
            m.put("riderName", rider != null ? rider.getRealName() : "未分配");

            if (o.getDeliverTime() != null && o.getEstimatedMinutes() != null) {
                long overtimeBy = java.time.Duration.between(
                        o.getDeliverTime().plusMinutes(o.getEstimatedMinutes() + 10), now).toMinutes();
                m.put("overtimeMinutes", Math.max(0, overtimeBy));
            }
            return m;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    @PutMapping("/merchant/{id}/status")
    public Result<?> toggleMerchantStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) return Result.fail("商家不存在");
        Integer newStatus = (Integer) body.get("status");
        if (newStatus == null || (newStatus != 1 && newStatus != 3)) {
            return Result.fail(400, "status 必须为 1(启用) 或 3(停用)");
        }
        merchant.setStatus(newStatus);
        merchantMapper.updateById(merchant);
        return Result.ok();
    }

    @PutMapping("/rider/{id}/status")
    public Result<?> toggleRiderStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Rider rider = riderMapper.selectById(id);
        if (rider == null) return Result.fail("骑手不存在");
        Integer newStatus = (Integer) body.get("status");
        if (newStatus == null || (newStatus != 1 && newStatus != 5)) {
            return Result.fail(400, "status 必须为 1(启用) 或 5(停用)");
        }
        rider.setAuditStatus(newStatus == 5 ? 2 : 1); // Disable → audit REJECTED, Enable → audit APPROVED
        rider.setStatus(newStatus == 5 ? 5 : 4);        // Disable → DISABLED, Enable → OFFLINE
        riderMapper.updateById(rider);
        return Result.ok();
    }

    @GetMapping("/recommend/stats")
    public Result<Map<String, Object>> recommendStats() {
        Map<String, Object> data = new HashMap<>();

        // Summary
        long totalDishes = dishMapper.selectCount(null);
        long totalCategories = categoryMapper.selectCount(null);
        data.put("totalDishes", totalDishes);
        data.put("totalCategories", totalCategories);

        // Hot dishes Top 10
        List<com.waimai.common.entity.Dish> hot = dishMapper.selectList(
                new LambdaQueryWrapper<com.waimai.common.entity.Dish>()
                        .eq(com.waimai.common.entity.Dish::getStatus, 1)
                        .orderByDesc(com.waimai.common.entity.Dish::getMonthlySales)
                        .last("LIMIT 10"));
        List<Map<String, Object>> hotList = hot.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("dishId", d.getId());
            m.put("name", d.getName());
            m.put("price", d.getPrice());
            m.put("monthlySales", d.getMonthlySales());
            m.put("image", d.getImage());
            com.waimai.common.entity.Merchant merchant = merchantMapper.selectById(d.getMerchantId());
            m.put("merchantName", merchant != null ? merchant.getName() : "未知");
            com.waimai.common.entity.Category cat = categoryMapper.selectById(d.getCategoryId());
            m.put("categoryName", cat != null ? cat.getName() : "");
            return m;
        }).collect(Collectors.toList());
        data.put("hotDishes", hotList);

        // Category distribution (for pie chart)
        List<com.waimai.common.entity.Category> cats = categoryMapper.selectList(null);
        List<Map<String, Object>> catDist = new ArrayList<>();
        for (com.waimai.common.entity.Category c : cats) {
            long count = dishMapper.selectCount(new LambdaQueryWrapper<com.waimai.common.entity.Dish>()
                    .eq(com.waimai.common.entity.Dish::getCategoryId, c.getId())
                    .eq(com.waimai.common.entity.Dish::getStatus, 1));
            Map<String, Object> m = new HashMap<>();
            m.put("name", c.getName());
            m.put("value", count);
            catDist.add(m);
        }
        data.put("categoryDistribution", catDist);

        // Price range distribution
        List<com.waimai.common.entity.Dish> allDishes = dishMapper.selectList(
                new LambdaQueryWrapper<com.waimai.common.entity.Dish>().eq(com.waimai.common.entity.Dish::getStatus, 1));
        long budgetCount = 0, midCount = 0, premiumCount = 0;
        for (com.waimai.common.entity.Dish d : allDishes) {
            double p = d.getPrice() != null ? d.getPrice().doubleValue() : 0;
            if (p < 15) budgetCount++;
            else if (p < 30) midCount++;
            else premiumCount++;
        }
        List<Map<String, Object>> priceDist = new ArrayList<>();
        priceDist.add(Map.of("name", "实惠 (<¥15)", "value", budgetCount));
        priceDist.add(Map.of("name", "适中 (¥15-30)", "value", midCount));
        priceDist.add(Map.of("name", "品质 (>¥30)", "value", premiumCount));
        data.put("priceDistribution", priceDist);

        return Result.ok(data);
    }
}
