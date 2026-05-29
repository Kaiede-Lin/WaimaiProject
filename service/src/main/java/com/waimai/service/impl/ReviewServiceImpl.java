package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waimai.common.constant.OrderStatus;
import com.waimai.common.dto.SubmitReviewDTO;
import com.waimai.common.entity.*;
import com.waimai.common.exception.BusinessException;
import com.waimai.common.vo.ReviewVO;
import com.waimai.service.mapper.*;
import com.waimai.service.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final RiderMapper riderMapper;
    private final RiderLevelServiceImpl riderLevelService;

    public ReviewServiceImpl(ReviewMapper reviewMapper, OrderMapper orderMapper,
                             UserMapper userMapper, MerchantMapper merchantMapper,
                             RiderMapper riderMapper, RiderLevelServiceImpl riderLevelService) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.riderMapper = riderMapper;
        this.riderLevelService = riderLevelService;
    }

    @Override
    @Transactional
    public void submitReview(Long userId, SubmitReviewDTO dto) {
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new BusinessException("订单未完成，无法评价");
        }

        // Check existing reviews for this order
        List<Review> existing = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, dto.getOrderId())
                .eq(Review::getUserId, userId));
        boolean hasMerchantReview = existing.stream().anyMatch(r -> "MERCHANT".equals(r.getType()));
        boolean hasRiderReview = existing.stream().anyMatch(r -> "RIDER".equals(r.getType()));

        // Submit merchant review
        if (dto.getMerchantRating() != null && dto.getMerchantRating() > 0) {
            if (hasMerchantReview) {
                throw new BusinessException("已评价过该商家");
            }
            Review review = new Review();
            review.setOrderId(dto.getOrderId());
            review.setUserId(userId);
            review.setMerchantId(order.getMerchantId());
            review.setRating(dto.getMerchantRating());
            review.setContent(dto.getMerchantContent());
            review.setImages(dto.getImages());
            review.setType("MERCHANT");
            reviewMapper.insert(review);

            // Recalculate merchant score
            recalculateMerchantScore(order.getMerchantId());
        }

        // Submit rider review
        if (dto.getRiderRating() != null && dto.getRiderRating() > 0) {
            if (order.getRiderId() == null) {
                throw new BusinessException("该订单无骑手配送");
            }
            if (hasRiderReview) {
                throw new BusinessException("已评价过该骑手");
            }
            Review review = new Review();
            review.setOrderId(dto.getOrderId());
            review.setUserId(userId);
            review.setRiderId(order.getRiderId());
            review.setRating(dto.getRiderRating());
            review.setContent(dto.getRiderContent());
            review.setImages(dto.getImages());
            review.setType("RIDER");
            reviewMapper.insert(review);

            // Recalculate rider score
            recalculateRiderScore(order.getRiderId());
            riderLevelService.recalculateLevel(order.getRiderId());
        }
    }

    @Override
    public Page<ReviewVO> listByMerchant(Long merchantId, int page, int size) {
        Page<Review> reviewPage = reviewMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getMerchantId, merchantId)
                        .eq(Review::getType, "MERCHANT")
                        .orderByDesc(Review::getCreateTime));

        return convertToVOPage(reviewPage);
    }

    @Override
    public Page<ReviewVO> listByRider(Long riderId, int page, int size) {
        Page<Review> reviewPage = reviewMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getRiderId, riderId)
                        .eq(Review::getType, "RIDER")
                        .orderByDesc(Review::getCreateTime));

        return convertToVOPage(reviewPage);
    }

    @Override
    public Map<String, Object> getRatingSummary(Long targetId, String type) {
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<Review>()
                .eq("MERCHANT".equals(type) ? Review::getMerchantId : Review::getRiderId, targetId)
                .eq(Review::getType, type);

        List<Review> reviews = reviewMapper.selectList(qw);
        Map<String, Object> summary = new HashMap<>();

        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        summary.put("average", BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        summary.put("total", reviews.size());

        // Rating distribution
        Map<Integer, Long> distribution = reviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));
        summary.put("distribution", distribution);

        return summary;
    }

    private void recalculateMerchantScore(Long merchantId) {
        Map<String, Object> summary = getRatingSummary(merchantId, "MERCHANT");
        BigDecimal avg = (BigDecimal) summary.get("average");
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant != null) {
            merchant.setScore(avg);
            merchantMapper.updateById(merchant);
        }
    }

    private void recalculateRiderScore(Long riderId) {
        Map<String, Object> summary = getRatingSummary(riderId, "RIDER");
        BigDecimal avg = (BigDecimal) summary.get("average");
        Rider rider = riderMapper.selectById(riderId);
        if (rider != null) {
            rider.setScore(avg);
            riderMapper.updateById(rider);
        }
    }

    @Override
    public List<Review> getOrderReviews(Long orderId, Long userId) {
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
                .eq(Review::getUserId, userId));
    }

    private Page<ReviewVO> convertToVOPage(Page<Review> reviewPage) {
        Page<ReviewVO> voPage = new Page<>(reviewPage.getCurrent(), reviewPage.getSize(), reviewPage.getTotal());
        voPage.setRecords(reviewPage.getRecords().stream().map(r -> {
            ReviewVO vo = new ReviewVO();
            vo.setId(r.getId());
            vo.setOrderId(r.getOrderId());
            vo.setRating(r.getRating());
            vo.setContent(r.getContent());
            vo.setImages(r.getImages());
            vo.setType(r.getType());
            vo.setCreateTime(r.getCreateTime());

            User user = userMapper.selectById(r.getUserId());
            if (user != null) {
                vo.setUserName(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }
}