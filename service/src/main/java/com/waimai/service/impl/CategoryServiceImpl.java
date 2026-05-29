package com.waimai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waimai.common.entity.Category;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.mapper.CategoryMapper;
import com.waimai.service.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> listByMerchant(Long merchantId) {
        return lambdaQuery()
                .eq(Category::getMerchantId, merchantId)
                .orderByAsc(Category::getSort)
                .list();
    }

    @Override
    public Category addCategory(Long merchantId, Category category) {
        category.setMerchantId(merchantId);
        category.setId(null);
        save(category);
        return category;
    }

    @Override
    public void updateCategory(Long merchantId, Category category) {
        Category exist = getById(category.getId());
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException("分类不存在");
        }
        category.setMerchantId(merchantId);
        updateById(category);
    }

    @Override
    public void deleteCategory(Long merchantId, Long categoryId) {
        Category exist = getById(categoryId);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException("分类不存在");
        }
        removeById(categoryId);
    }
}
