package com.waimai.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waimai.common.Result;
import com.waimai.common.entity.Coupon;
import com.waimai.common.utils.UserContext;
import com.waimai.service.service.CouponService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // ── Admin endpoints ─────────────────────────────────────────────

    @PostMapping("/api/admin/coupon")
    public Result<Coupon> create(@RequestBody Coupon coupon) {
        return Result.ok(couponService.createCoupon(coupon));
    }

    @GetMapping("/api/admin/coupon/list")
    public Result<Page<Coupon>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return Result.ok(couponService.listCoupons(page, size));
    }

    @PutMapping("/api/admin/coupon/{id}/status")
    public Result<?> toggleStatus(@PathVariable Long id) {
        couponService.toggleStatus(id);
        return Result.ok();
    }

    // ── User endpoints ──────────────────────────────────────────────

    @GetMapping("/api/coupon/available")
    public Result<List<Map<String, Object>>> available(
            @RequestParam Long merchantId,
            @RequestParam java.math.BigDecimal orderAmount) {
        return Result.ok(couponService.availableCoupons(UserContext.getUserId(), merchantId, orderAmount));
    }

    @PostMapping("/api/coupon/receive/{couponId}")
    public Result<?> receive(@PathVariable Long couponId) {
        couponService.receiveCoupon(UserContext.getUserId(), couponId);
        return Result.ok();
    }

    @GetMapping("/api/coupon/my")
    public Result<List<Map<String, Object>>> myCoupons() {
        return Result.ok(couponService.myCoupons(UserContext.getUserId()));
    }
}
