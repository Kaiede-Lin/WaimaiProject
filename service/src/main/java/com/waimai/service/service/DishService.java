package com.waimai.service.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.waimai.common.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    List<Dish> listByCategory(Long merchantId, Long categoryId);

    Page<Dish> pageByMerchant(Long merchantId, int page, int size, Integer status);

    void addDish(Long merchantId, Dish dish);

    void updateDish(Long merchantId, Dish dish);

    void updateStatus(Long merchantId, Long dishId, Integer status);

    void deleteDish(Long merchantId, Long dishId);
}
