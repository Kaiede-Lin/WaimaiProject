package com.waimai.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.waimai.common.entity.UserAddress;

import java.util.List;

public interface UserAddressService extends IService<UserAddress> {

    List<UserAddress> listByUserId(Long userId);

    void saveAddress(Long userId, UserAddress addr);

    void updateAddress(Long userId, Long addressId, UserAddress addr);

    void deleteAddress(Long userId, Long addressId);
}
