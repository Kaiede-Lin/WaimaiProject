package com.waimai.api.controller;

import com.waimai.common.Result;
import com.waimai.common.entity.Category;
import com.waimai.service.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/{merchantId}/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<Category>> list(@PathVariable Long merchantId) {
        return Result.ok(categoryService.listByMerchant(merchantId));
    }

    @PostMapping
    public Result<Category> add(@PathVariable Long merchantId, @RequestBody Category category) {
        return Result.ok(categoryService.addCategory(merchantId, category));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long merchantId, @PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.updateCategory(merchantId, category);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long merchantId, @PathVariable Long id) {
        categoryService.deleteCategory(merchantId, id);
        return Result.ok();
    }
}
