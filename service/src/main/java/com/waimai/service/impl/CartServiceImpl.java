package com.waimai.service.impl;

import com.waimai.common.entity.Dish;
import com.waimai.common.entity.Merchant;
import com.waimai.common.exception.BusinessException;
import com.waimai.common.vo.CartItemVO;
import com.waimai.common.vo.CartVO;
import com.waimai.service.service.CartService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    private static final String CART_KEY_PREFIX = "waimai:cart:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final DishServiceImpl dishService;
    private final MerchantServiceImpl merchantService;

    public CartServiceImpl(RedisTemplate<String, Object> redisTemplate,
                           DishServiceImpl dishService,
                           MerchantServiceImpl merchantService) {
        this.redisTemplate = redisTemplate;
        this.dishService = dishService;
        this.merchantService = merchantService;
    }

    @Override
    public void addItem(Long userId, Long dishId, Integer quantity) {
        Dish dish = dishService.getById(dishId);
        if (dish == null || dish.getStatus() != 1) {
            throw new BusinessException("菜品已下架");
        }

        String key = CART_KEY_PREFIX + userId;
        Integer currentQty = (Integer) redisTemplate.opsForHash().get(key, dishId.toString());
        if (currentQty == null) currentQty = 0;

        int newQty = currentQty + quantity;
        if (newQty > dish.getStock()) {
            throw new BusinessException("库存不足，当前库存: " + dish.getStock());
        }
        redisTemplate.opsForHash().put(key, dishId.toString(), newQty);
    }

    @Override
    public void removeItem(Long userId, Long dishId) {
        redisTemplate.opsForHash().delete(CART_KEY_PREFIX + userId, dishId.toString());
    }

    @Override
    public void updateQuantity(Long userId, Long dishId, Integer quantity) {
        if (quantity <= 0) {
            removeItem(userId, dishId);
            return;
        }
        Dish dish = dishService.getById(dishId);
        if (dish != null && quantity > dish.getStock()) {
            throw new BusinessException("库存不足");
        }
        redisTemplate.opsForHash().put(CART_KEY_PREFIX + userId, dishId.toString(), quantity);
    }

    @Override
    public void clearCart(Long userId) {
        redisTemplate.delete(CART_KEY_PREFIX + userId);
    }

    @Override
    public CartVO getCart(Long userId) {
        String key = CART_KEY_PREFIX + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        CartVO cart = new CartVO();
        List<CartItemVO> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Long merchantId = null;

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long dishId = Long.valueOf(String.valueOf(entry.getKey()));
            Integer qty = Integer.valueOf(String.valueOf(entry.getValue()));
            Dish dish = dishService.getById(dishId);
            if (dish == null || dish.getStatus() != 1) {
                continue;
            }

            if (merchantId == null) {
                merchantId = dish.getMerchantId();
            } else if (!merchantId.equals(dish.getMerchantId())) {
                throw new BusinessException("购物车中只能包含同一商家的菜品");
            }

            CartItemVO item = new CartItemVO();
            item.setDishId(dishId);
            item.setDishName(dish.getName());
            item.setDishImage(dish.getImage());
            item.setPrice(dish.getPrice());
            item.setQuantity(qty);
            item.setStock(dish.getStock());
            items.add(item);

            totalAmount = totalAmount.add(dish.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        cart.setItems(items);
        cart.setTotalAmount(totalAmount);
        cart.setMerchantId(merchantId);

        if (merchantId != null) {
            Merchant merchant = merchantService.getById(merchantId);
            if (merchant != null) {
                cart.setMerchantName(merchant.getName());
                cart.setDeliveryFee(merchant.getDeliveryFee() != null ? merchant.getDeliveryFee() : new BigDecimal("0"));
            }
        }
        cart.setTotal(cart.getTotal()); // pre-compute total

        return cart;
    }
}
