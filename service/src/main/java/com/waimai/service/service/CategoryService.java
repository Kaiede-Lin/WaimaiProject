package com.waimai.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.waimai.common.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> listByMerchant(Long merchantId);

    Category addCategory(Long merchantId, Category category);

    void updateCategory(Long merchantId, Category category);

    void deleteCategory(Long merchantId, Long categoryId);
}
