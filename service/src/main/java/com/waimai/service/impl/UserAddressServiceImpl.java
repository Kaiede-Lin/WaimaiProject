package com.waimai.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waimai.common.entity.UserAddress;
import com.waimai.common.exception.BusinessException;
import com.waimai.service.mapper.UserAddressMapper;
import com.waimai.service.service.UserAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

    @Override
    public List<UserAddress> listByUserId(Long userId) {
        return lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime)
                .list();
    }

    @Override
    @Transactional
    public void saveAddress(Long userId, UserAddress addr) {
        addr.setUserId(userId);
        if (Boolean.TRUE.equals(addr.getIsDefault())) {
            clearDefault(userId);
        }
        save(addr);
    }

    @Override
    @Transactional
    public void updateAddress(Long userId, Long addressId, UserAddress addr) {
        UserAddress existing = getById(addressId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        addr.setId(addressId);
        addr.setUserId(userId);
        if (Boolean.TRUE.equals(addr.getIsDefault())) {
            clearDefault(userId);
        }
        updateById(addr);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress existing = getById(addressId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        removeById(addressId);
    }

    private void clearDefault(Long userId) {
        update(new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, true)
                .set(UserAddress::getIsDefault, false));
    }
}
