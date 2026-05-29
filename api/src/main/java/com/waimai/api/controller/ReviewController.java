package com.waimai.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waimai.common.Result;
import com.waimai.common.dto.SubmitReviewDTO;
import com.waimai.common.utils.UserContext;
import com.waimai.common.vo.ReviewVO;
import com.waimai.service.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/submit")
    public Result<?> submit(@Valid @RequestBody SubmitReviewDTO dto) {
        reviewService.submitReview(UserContext.getUserId(), dto);
        return Result.ok();
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<Page<ReviewVO>> merchantReviews(
            @PathVariable Long merchantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reviewService.listByMerchant(merchantId, page, size));
    }

    @GetMapping("/rider/{riderId}")
    public Result<Page<ReviewVO>> riderReviews(
            @PathVariable Long riderId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reviewService.listByRider(riderId, page, size));
    }

    @GetMapping("/order/{orderId}")
    public Result<?> orderReview(@PathVariable Long orderId) {
        return Result.ok(reviewService.getOrderReviews(orderId, UserContext.getUserId()));
    }
}