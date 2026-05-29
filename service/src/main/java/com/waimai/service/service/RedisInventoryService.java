package com.waimai.service.service;

import java.util.List;

public interface RedisInventoryService {

    void loadMerchantInventory(Long merchantId);

    void syncDishStock(Long merchantId, Long dishId, Integer stock);

    List<Long> deductInventory(Long merchantId, List<Long> dishIds, List<Integer> quantities);

    void rollbackInventory(Long merchantId, List<Long> dishIds, List<Integer> quantities);

    /**
     * 单菜品原子扣减 — 使用 opsForValue().decrement()，适合高并发场景。
     * 需要事先通过 loadSingleDishStock() 初始化 key。
     *
     * @return 扣减后的剩余库存
     */
    Long decrementStock(Long merchantId, Long dishId, int quantity);

    /**
     * 初始化单个菜品的独立库存 key（配合 decrementStock 使用）
     */
    void loadSingleDishStock(Long merchantId, Long dishId, Integer stock);
}
