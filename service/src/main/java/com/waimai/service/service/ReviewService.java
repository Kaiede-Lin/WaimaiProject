package com.waimai.service.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waimai.common.dto.SubmitReviewDTO;
import com.waimai.common.entity.Review;
import com.waimai.common.vo.ReviewVO;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    void submitReview(Long userId, SubmitReviewDTO dto);
    Page<ReviewVO> listByMerchant(Long merchantId, int page, int size);
    Page<ReviewVO> listByRider(Long riderId, int page, int size);
    Map<String, Object> getRatingSummary(Long targetId, String type);
    List<Review> getOrderReviews(Long orderId, Long userId);

    Page<ReviewVO> listByUser(Long userId, int page, int size);
}