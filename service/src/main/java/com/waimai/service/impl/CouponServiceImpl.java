package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waimai.common.entity.Coupon;
import com.waimai.common.entity.UserCoupon;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.mapper.CouponMapper;
import com.waimai.service.mapper.UserCouponMapper;
import com.waimai.service.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public CouponServiceImpl(CouponMapper couponMapper, UserCouponMapper userCouponMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    public Coupon createCoupon(Coupon coupon) {
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        couponMapper.insert(coupon);
        return coupon;
    }

    @Override
    public Page<Coupon> listCoupons(int page, int size) {
        return couponMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getCreateTime));
    }

    @Override
    public void toggleStatus(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new BusinessException("优惠券不存在");
        coupon.setStatus(coupon.getStatus() == 1 ? 0 : 1);
        couponMapper.updateById(coupon);
    }

    @Override
    public List<Map<String, Object>> availableCoupons(Long userId, Long merchantId, BigDecimal orderAmount) {
        List<Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .and(w -> w.isNull(Coupon::getMerchantId).or().eq(Coupon::getMerchantId, merchantId)));

        // Filter: received < total
        coupons.removeIf(c -> c.getReceivedCount() >= c.getTotalCount());

        List<Long> userCouponIds = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getUserId, userId))
                .stream().map(UserCoupon::getCouponId).collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Coupon c : coupons) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("type", c.getType());
            m.put("threshold", c.getThreshold());
            m.put("discountValue", c.getDiscountValue());
            m.put("validDays", c.getValidDays());
            m.put("received", userCouponIds.contains(c.getId()));
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional
    public UserCoupon receiveCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) throw new BusinessException("优惠券不存在或已下架");
        if (coupon.getReceivedCount() >= coupon.getTotalCount()) throw new BusinessException("优惠券已被抢光");

        long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getCouponId, couponId));
        if (count > 0) throw new BusinessException("已领取过该优惠券");

        coupon.setReceivedCount(coupon.getReceivedCount() + 1);
        couponMapper.updateById(coupon);

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus("UNUSED");
        uc.setExpireTime(LocalDateTime.now().plusDays(coupon.getValidDays()));
        userCouponMapper.insert(uc);
        return uc;
    }

    @Override
    public List<Map<String, Object>> myCoupons(Long userId) {
        List<UserCoupon> ucs = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getUserId, userId)
                        .orderByDesc(UserCoupon::getReceiveTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCoupon uc : ucs) {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon == null) continue;

            // Mark expired
            if ("UNUSED".equals(uc.getStatus()) && uc.getExpireTime().isBefore(LocalDateTime.now())) {
                uc.setStatus("EXPIRED");
                userCouponMapper.updateById(uc);
            }

            Map<String, Object> m = new HashMap<>();
            m.put("id", uc.getId());
            m.put("couponId", coupon.getId());
            m.put("name", coupon.getName());
            m.put("type", coupon.getType());
            m.put("threshold", coupon.getThreshold());
            m.put("discountValue", coupon.getDiscountValue());
            m.put("status", uc.getStatus());
            m.put("expireTime", uc.getExpireTime());
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional
    public BigDecimal applyCoupon(Long userCouponId, Long userId, BigDecimal orderAmount) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) throw new BusinessException("优惠券不存在");
        if (!"UNUSED".equals(uc.getStatus())) throw new BusinessException("优惠券不可用");
        if (uc.getExpireTime().isBefore(LocalDateTime.now())) {
            uc.setStatus("EXPIRED");
            userCouponMapper.updateById(uc);
            throw new BusinessException("优惠券已过期");
        }

        Coupon coupon = couponMapper.selectById(uc.getCouponId());
        if (coupon == null) throw new BusinessException("优惠券不存在");

        BigDecimal discount = BigDecimal.ZERO;
        switch (coupon.getType()) {
            case "FULL_REDUCTION":
                if (coupon.getThreshold() != null && orderAmount.compareTo(coupon.getThreshold()) < 0) {
                    throw new BusinessException("未达到满减门槛 ¥" + coupon.getThreshold());
                }
                discount = coupon.getDiscountValue();
                break;
            case "DISCOUNT":
                discount = orderAmount.multiply(BigDecimal.ONE.subtract(
                        coupon.getDiscountValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)));
                break;
            case "FREE_DELIVERY":
                discount = coupon.getDiscountValue(); // delivery fee discount
                break;
        }

        uc.setStatus("USED");
        uc.setUseTime(LocalDateTime.now());
        userCouponMapper.updateById(uc);

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponMapper.updateById(coupon);

        return discount;
    }
}
