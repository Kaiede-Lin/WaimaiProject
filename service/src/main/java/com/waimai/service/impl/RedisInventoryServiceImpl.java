package com.waimai.service.impl;

import com.waimai.common.entity.Dish;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.service.RedisInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RedisInventoryServiceImpl implements RedisInventoryService {

    private static final Logger log = LoggerFactory.getLogger(RedisInventoryServiceImpl.class);

    private static final String INVENTORY_HASH_PREFIX = "waimai:inventory:";
    private static final String STOCK_KEY_PREFIX = "waimai:stock:";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> deductInventoryScript;
    private final DefaultRedisScript<Long> rollbackInventoryScript;
    private final DishServiceImpl dishService;

    public RedisInventoryServiceImpl(StringRedisTemplate redis,
                                      DefaultRedisScript<List> deductInventoryScript,
                                      DefaultRedisScript<Long> rollbackInventoryScript,
                                      DishServiceImpl dishService) {
        this.redis = redis;
        this.deductInventoryScript = deductInventoryScript;
        this.rollbackInventoryScript = rollbackInventoryScript;
        this.dishService = dishService;
    }

    // ─── Hash-based (Lua) — 批量扣减，保证多菜品原子性 ──────────────

    @Override
    public void loadMerchantInventory(Long merchantId) {
        String key = INVENTORY_HASH_PREFIX + merchantId;
        redis.delete(key);
        List<Dish> dishes = dishService.pageByMerchant(merchantId, 1, 500, 1).getRecords();
        for (Dish dish : dishes) {
            redis.opsForHash().put(key, dish.getId().toString(), String.valueOf(dish.getStock()));
        }
        log.info("Loaded {} dishes inventory for merchant {}", dishes.size(), merchantId);
    }

    @Override
    public void syncDishStock(Long merchantId, Long dishId, Integer stock) {
        String key = INVENTORY_HASH_PREFIX + merchantId;
        redis.opsForHash().put(key, dishId.toString(), String.valueOf(stock));
    }

    @Override
    public List<Long> deductInventory(Long merchantId, List<Long> dishIds, List<Integer> quantities) {
        String key = INVENTORY_HASH_PREFIX + merchantId;

        // 懒加载：Redis 中缺失的菜品自动从 MySQL 回填
        ensureInventoryLoaded(merchantId, key, dishIds);

        List<String> args = new ArrayList<>();
        for (int i = 0; i < dishIds.size(); i++) {
            args.add(dishIds.get(i).toString());
            args.add(quantities.get(i).toString());
        }

        List<Object> result = redis.execute(deductInventoryScript,
                Collections.singletonList(key),
                args.toArray());

        if (result != null && !result.isEmpty()) {
            long code = toLong(result.get(0));
            if (code == 1) {
                long dishId = toLong(result.get(1));
                long stock = toLong(result.get(2));
                throw new BusinessException("菜品ID " + dishId + " 库存不足，当前库存: " + stock);
            }
            if (code == 2) {
                long dishId = toLong(result.get(1));
                throw new BusinessException("菜品ID " + dishId + " 不存在，无法扣减库存");
            }
        }
        return dishIds;
    }

    @Override
    public void rollbackInventory(Long merchantId, List<Long> dishIds, List<Integer> quantities) {
        String key = INVENTORY_HASH_PREFIX + merchantId;
        List<String> args = new ArrayList<>();
        for (int i = 0; i < dishIds.size(); i++) {
            args.add(dishIds.get(i).toString());
            args.add(quantities.get(i).toString());
        }
        redis.execute(rollbackInventoryScript,
                Collections.singletonList(key),
                args.toArray());
    }

    // ─── String-based (decrement) — 单菜品原子扣减 ──────────────────

    @Override
    public void loadSingleDishStock(Long merchantId, Long dishId, Integer stock) {
        String key = STOCK_KEY_PREFIX + merchantId + ":" + dishId;
        redis.opsForValue().set(key, String.valueOf(stock));
    }

    @Override
    public Long decrementStock(Long merchantId, Long dishId, int quantity) {
        String key = STOCK_KEY_PREFIX + merchantId + ":" + dishId;

        // 懒加载：key 不存在则从 MySQL 回填
        if (Boolean.FALSE.equals(redis.hasKey(key))) {
            Dish dish = dishService.getById(dishId);
            if (dish == null) {
                throw new BusinessException("菜品ID " + dishId + " 不存在");
            }
            redis.opsForValue().set(key, String.valueOf(dish.getStock()));
            log.info("Lazy-loaded single dish stock from MySQL: dishId={} stock={}", dishId, dish.getStock());
        }

        Long remaining = redis.opsForValue().decrement(key, quantity);

        if (remaining == null) {
            throw new BusinessException("菜品ID " + dishId + " 库存扣减失败");
        }

        if (remaining < 0) {
            redis.opsForValue().increment(key, quantity);
            throw new BusinessException("菜品ID " + dishId + " 库存不足");
        }

        return remaining;
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    /**
     * 懒加载策略：遍历待扣减的菜品，若 Redis Hash 中不存在则从 MySQL 查询并回填。
     * 在 Lua 脚本执行前调用，确保所有菜品库存已就绪。
     */
    private void ensureInventoryLoaded(Long merchantId, String hashKey, List<Long> dishIds) {
        for (Long dishId : dishIds) {
            if (Boolean.FALSE.equals(redis.opsForHash().hasKey(hashKey, dishId.toString()))) {
                Dish dish = dishService.getById(dishId);
                if (dish != null && dish.getStatus() != null && dish.getStatus() == 1) {
                    redis.opsForHash().put(hashKey, dishId.toString(), String.valueOf(dish.getStock()));
                    log.info("Lazy-loaded dish stock from MySQL: merchantId={} dishId={} stock={}",
                            merchantId, dishId, dish.getStock());
                }
            }
        }
    }

    /**
     * 安全地将 Redis/Lua 脚本返回值转为 long。
     * StringRedisTemplate 下 Lua 返回整数是 Long 类型，但以防序列化器变更导致返回 String，做兼容处理。
     */
    private long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
