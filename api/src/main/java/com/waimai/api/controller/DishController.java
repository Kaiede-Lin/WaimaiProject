package com.waimai.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waimai.common.Result;
import com.waimai.common.dto.DishDTO;
import com.waimai.common.entity.Dish;
import com.waimai.service.service.DishService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant")
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping("/{merchantId}/dishes")
    public Result<Page<Dish>> listDishes(@PathVariable Long merchantId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(dishService.pageByMerchant(merchantId, page, size, status));
    }

    @GetMapping("/{merchantId}/dishes/category/{categoryId}")
    public Result<java.util.List<Dish>> listByCategory(@PathVariable Long merchantId, @PathVariable Long categoryId) {
        return Result.ok(dishService.listByCategory(merchantId, categoryId));
    }

    @PostMapping("/{merchantId}/dishes")
    public Result<?> addDish(@PathVariable Long merchantId, @Valid @RequestBody DishDTO dto) {
        Dish dish = new Dish();
        dish.setName(dto.getName());
        dish.setImage(dto.getImage());
        dish.setPrice(dto.getPrice());
        dish.setOriginalPrice(dto.getOriginalPrice());
        dish.setRichDescription(dto.getRichDescription());
        dish.setSummary(dto.getSummary());
        dish.setStock(dto.getStock());
        dish.setSort(dto.getSort());
        dish.setCategoryId(dto.getCategoryId());
        dishService.addDish(merchantId, dish);
        return Result.ok();
    }

    @PutMapping("/{merchantId}/dishes/{dishId}")
    public Result<?> updateDish(@PathVariable Long merchantId, @PathVariable Long dishId,
                                 @Valid @RequestBody DishDTO dto) {
        Dish dish = new Dish();
        dish.setId(dishId);
        dish.setName(dto.getName());
        dish.setImage(dto.getImage());
        dish.setPrice(dto.getPrice());
        dish.setOriginalPrice(dto.getOriginalPrice());
        dish.setRichDescription(dto.getRichDescription());
        dish.setSummary(dto.getSummary());
        dish.setStock(dto.getStock());
        dish.setSort(dto.getSort());
        dish.setCategoryId(dto.getCategoryId());
        dishService.updateDish(merchantId, dish);
        return Result.ok();
    }

    @PutMapping("/{merchantId}/dishes/{dishId}/status")
    public Result<?> updateStatus(@PathVariable Long merchantId, @PathVariable Long dishId,
                                   @RequestParam Integer status) {
        dishService.updateStatus(merchantId, dishId, status);
        return Result.ok();
    }

    @DeleteMapping("/{merchantId}/dishes/{dishId}")
    public Result<?> deleteDish(@PathVariable Long merchantId, @PathVariable Long dishId) {
        dishService.deleteDish(merchantId, dishId);
        return Result.ok();
    }
}
