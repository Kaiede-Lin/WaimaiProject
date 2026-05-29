package com.waimai.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waimai.common.Result;
import com.waimai.common.entity.Category;
import com.waimai.common.entity.Dish;
import com.waimai.common.entity.Merchant;
import com.waimai.common.entity.Order;
import com.waimai.common.exception.BusinessException;
import com.waimai.common.utils.UserContext;
import com.waimai.common.vo.MerchantNearbyVO;
import com.waimai.common.vo.MerchantReportVO;
import com.waimai.common.vo.OrderVO;
import com.waimai.service.service.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantService merchantService;
    private final GeoService geoService;
    private final DishService dishService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final DispatchService dispatchService;
    private final ReportService reportService;

    public MerchantController(MerchantService merchantService, GeoService geoService,
                               DishService dishService, CategoryService categoryService,
                               OrderService orderService, DispatchService dispatchService,
                               ReportService reportService) {
        this.merchantService = merchantService;
        this.geoService = geoService;
        this.dishService = dishService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.dispatchService = dispatchService;
        this.reportService = reportService;
    }

    // ─── Public endpoints ──────────────────────────────────────────

    @GetMapping("/nearby")
    public Result<List<MerchantNearbyVO>> nearby(@RequestParam double lng, @RequestParam double lat,
                                                  @RequestParam(defaultValue = "5") double radius) {
        return Result.ok(geoService.searchNearbyMerchants(lng, lat, radius));
    }

    @GetMapping("/{id}")
    public Result<Merchant> info(@PathVariable Long id) {
        return Result.ok(merchantService.getById(id));
    }

    @GetMapping("/apply/status")
    public Result<Map<String, Object>> applyStatus(@RequestParam String code) {
        String openid = "wx_" + code;
        Merchant merchant = merchantService.getByOpenid(openid);
        Map<String, Object> result = new HashMap<>();
        if (merchant == null) {
            result.put("applied", false);
            result.put("status", -1);
            result.put("statusText", "未申请");
        } else {
            result.put("applied", true);
            result.put("status", merchant.getStatus());
            result.put("statusText", merchantStatusText(merchant.getStatus()));
            result.put("rejectionReason", merchant.getRejectionReason());
            result.put("name", merchant.getName());
            result.put("phone", merchant.getPhone());
        }
        return Result.ok(result);
    }

    @PostMapping("/apply")
    public Result<?> apply(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        if (code == null || code.isBlank()) {
            return Result.fail(400, "code不能为空");
        }
        String openid = "wx_" + code;

        // Prevent duplicate application
        Merchant exist = merchantService.getByOpenid(openid);
        if (exist != null && exist.getStatus() != com.waimai.common.constant.MerchantStatus.REJECTED) {
            throw new BusinessException("您已申请过，请勿重复申请");
        }

        Merchant merchant = new Merchant();
        merchant.setOpenid(openid);
        merchant.setName((String) body.get("name"));
        merchant.setLogo((String) body.get("logo"));
        merchant.setPhone((String) body.get("phone"));
        merchant.setAddress((String) body.get("address"));

        Object lngObj = body.get("longitude");
        if (lngObj != null) merchant.setLongitude(new java.math.BigDecimal(lngObj.toString()));
        Object latObj = body.get("latitude");
        if (latObj != null) merchant.setLatitude(new java.math.BigDecimal(latObj.toString()));

        merchant.setDescription((String) body.get("description"));
        merchant.setBusinessHours((String) body.getOrDefault("businessHours", "09:00-22:00"));

        merchantService.apply(merchant);
        return Result.ok(merchant.getId());
    }

    // ─── Current merchant profile ──────────────────────────────────

    @GetMapping("/info")
    public Result<Merchant> myInfo() {
        return Result.ok(currentMerchant());
    }

    @PutMapping("/info")
    public Result<?> updateInfo(@RequestBody Merchant merchant) {
        Merchant cur = currentMerchant();
        merchant.setId(cur.getId());
        merchantService.updateById(merchant);
        return Result.ok();
    }

    @GetMapping("/my")
    public Result<Merchant> my() {
        return Result.ok(currentMerchant());
    }

    // ─── Dish management (current merchant) ────────────────────────

    @GetMapping("/dish/list")
    public Result<List<Dish>> dishList(@RequestParam(required = false) Long categoryId) {
        Long mchId = currentMerchantId();
        if (categoryId != null) {
            return Result.ok(dishService.listByCategory(mchId, categoryId));
        }
        Page<Dish> page = dishService.pageByMerchant(mchId, 1, 200, null);
        return Result.ok(page.getRecords());
    }

    @PostMapping("/dish")
    public Result<?> addDish(@RequestBody Dish dish) {
        dishService.addDish(currentMerchantId(), dish);
        return Result.ok();
    }

    @PutMapping("/dish/{id}")
    public Result<?> updateDish(@PathVariable Long id, @RequestBody Dish dish) {
        dish.setId(id);
        dishService.updateDish(currentMerchantId(), dish);
        return Result.ok();
    }

    @DeleteMapping("/dish/{id}")
    public Result<?> deleteDish(@PathVariable Long id) {
        dishService.deleteDish(currentMerchantId(), id);
        return Result.ok();
    }

    // ─── Category management (current merchant) ────────────────────

    @GetMapping("/category/list")
    public Result<List<Category>> categoryList() {
        return Result.ok(categoryService.listByMerchant(currentMerchantId()));
    }

    @PostMapping("/category")
    public Result<Category> addCategory(@RequestBody Category category) {
        return Result.ok(categoryService.addCategory(currentMerchantId(), category));
    }

    @PutMapping("/category/{id}")
    public Result<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.updateCategory(currentMerchantId(), category);
        return Result.ok();
    }

    @DeleteMapping("/category/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(currentMerchantId(), id);
        return Result.ok();
    }

    // ─── Order management (current merchant) ───────────────────────

    @GetMapping("/order/list")
    public Result<List<Order>> orderList(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        Page<Order> result = orderService.listByMerchant(currentMerchantId(), page, size);
        return Result.ok(result.getRecords());
    }

    @PostMapping("/order/{id}/accept")
    public Result<?> acceptOrder(@PathVariable Long id) {
        orderService.acceptOrder(id, currentMerchantId());
        return Result.ok();
    }

    @PostMapping("/order/{id}/complete")
    public Result<Long> completeOrder(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null || !order.getMerchantId().equals(currentMerchantId())) {
            throw new BusinessException("订单不存在");
        }
        Long riderId = dispatchService.dispatchOrder(id);
        return Result.ok(riderId);
    }

    @PostMapping("/order/{id}/cancel")
    public Result<?> cancelOrder(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null || !order.getMerchantId().equals(currentMerchantId())) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(com.waimai.common.constant.OrderStatus.CANCELLED);
        orderService.updateById(order);
        return Result.ok();
    }

    // ─── Business Hours & Reports ─────────────────────────────────

    @PutMapping("/business-hours")
    public Result<?> updateBusinessHours(@RequestBody Map<String, String> body) {
        Merchant cur = currentMerchant();
        String hours = body.get("businessHours");
        if (hours != null && !hours.isBlank()) {
            cur.setBusinessHours(hours);
            merchantService.updateById(cur);
        }
        return Result.ok();
    }

    @GetMapping("/report/daily")
    public Result<MerchantReportVO> dailyReport(@RequestParam String date) {
        return Result.ok(reportService.dailyReport(currentMerchantId(), LocalDate.parse(date)));
    }

    @GetMapping("/report/weekly")
    public Result<MerchantReportVO> weeklyReport(@RequestParam String startDate) {
        return Result.ok(reportService.weeklyReport(currentMerchantId(), LocalDate.parse(startDate)));
    }

    @GetMapping("/report/monthly")
    public Result<MerchantReportVO> monthlyReport(@RequestParam String month) {
        return Result.ok(reportService.monthlyReport(currentMerchantId(), month));
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private Merchant currentMerchant() {
        Merchant m = merchantService.getByOpenid(UserContext.getOpenid());
        if (m == null) throw new BusinessException("未找到商家信息");
        return m;
    }

    private Long currentMerchantId() {
        return currentMerchant().getId();
    }

    private String merchantStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "审核通过";
            case 2 -> "审核拒绝";
            case 3 -> "已停用";
            default -> "未知";
        };
    }
}
