package com.waimai.service.service;

import java.util.Map;

public interface RecommendService {
    Map<String, Object> chat(Long userId, String message);
}
