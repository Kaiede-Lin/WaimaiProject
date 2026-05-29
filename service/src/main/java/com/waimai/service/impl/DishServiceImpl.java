package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waimai.common.entity.Dish;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.mapper.DishMapper;
import com.waimai.service.service.DishService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Override
    public List<Dish> listByCategory(Long merchantId, Long categoryId) {
        return lambdaQuery()
                .eq(Dish::getMerchantId, merchantId)
                .eq(Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, 1)
                .orderByAsc(Dish::getSort)
                .list();
    }

    @Override
    public Page<Dish> pageByMerchant(Long merchantId, int page, int size, Integer status) {
        var wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getMerchantId, merchantId);
        if (status != null) {
            wrapper.eq(Dish::getStatus, status);
        }
        wrapper.orderByAsc(Dish::getSort);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public void addDish(Long merchantId, Dish dish) {
        dish.setMerchantId(merchantId);
        dish.setMonthlySales(0);
        if (dish.getStatus() == null) dish.setStatus(1);
        dish.setId(null);
        save(dish);
    }

    @Override
    public void updateDish(Long merchantId, Dish dish) {
        Dish exist = getById(dish.getId());
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException("菜品不存在");
        }
        dish.setMerchantId(merchantId);
        updateById(dish);
    }

    @Override
    public void updateStatus(Long merchantId, Long dishId, Integer status) {
        Dish dish = getById(dishId);
        if (dish == null || !dish.getMerchantId().equals(merchantId)) {
            throw new BusinessException("菜品不存在");
        }
        dish.setStatus(status);
        updateById(dish);
    }

    @Override
    public void deleteDish(Long merchantId, Long dishId) {
        Dish dish = getById(dishId);
        if (dish == null || !dish.getMerchantId().equals(merchantId)) {
            throw new BusinessException("菜品不存在");
        }
        removeById(dishId);
    }
}
