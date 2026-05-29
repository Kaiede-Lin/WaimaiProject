package com.waimai.api.controller;

import com.waimai.common.Result;
import com.waimai.common.utils.UserContext;
import com.waimai.common.vo.CartVO;
import com.waimai.service.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Result<CartVO> list() {
        return Result.ok(cartService.getCart(UserContext.getUserId()));
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody Map<String, Object> body) {
        Long dishId = Long.valueOf(body.get("dishId").toString());
        Integer quantity = body.get("quantity") != null
                ? Integer.valueOf(body.get("quantity").toString()) : 1;
        cartService.addItem(UserContext.getUserId(), dishId, quantity);
        return Result.ok();
    }

    @PostMapping("/remove")
    public Result<?> remove(@RequestBody Map<String, Object> body) {
        Long dishId = Long.valueOf(body.get("dishId").toString());
        cartService.removeItem(UserContext.getUserId(), dishId);
        return Result.ok();
    }

    @PutMapping("/{dishId}")
    public Result<?> updateQty(@PathVariable Long dishId, @RequestParam Integer quantity) {
        cartService.updateQuantity(UserContext.getUserId(), dishId, quantity);
        return Result.ok();
    }

    @DeleteMapping("/clear")
    public Result<?> clear() {
        cartService.clearCart(UserContext.getUserId());
        return Result.ok();
    }
}
