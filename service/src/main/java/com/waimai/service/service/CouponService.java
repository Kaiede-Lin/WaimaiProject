package com.waimai.service.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waimai.common.entity.Coupon;
import com.waimai.common.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CouponService {
    Coupon createCoupon(Coupon coupon);
    Page<Coupon> listCoupons(int page, int size);
    void toggleStatus(Long id);
    List<Map<String, Object>> availableCoupons(Long userId, Long merchantId, BigDecimal orderAmount);
    UserCoupon receiveCoupon(Long userId, Long couponId);
    List<Map<String, Object>> myCoupons(Long userId);
    BigDecimal applyCoupon(Long userCouponId, Long userId, BigDecimal orderAmount);
}
