package com.waimai.api.controller;

import com.waimai.common.Result;
import com.waimai.common.utils.UserContext;
import com.waimai.service.service.RecommendService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        return Result.ok(recommendService.chat(UserContext.getUserId(), message));
    }
}
