package com.waimai.service.service;

import com.waimai.common.vo.CartVO;

public interface CartService {

    void addItem(Long userId, Long dishId, Integer quantity);

    void removeItem(Long userId, Long dishId);

    void updateQuantity(Long userId, Long dishId, Integer quantity);

    void clearCart(Long userId);

    CartVO getCart(Long userId);
}
