package com.waimai.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.waimai.common.entity.Merchant;

import java.util.List;

public interface MerchantService extends IService<Merchant> {

    Merchant getByOpenid(String openid);

    Merchant apply(Merchant merchant);

    void audit(Long merchantId, Integer status, String reason);

    List<Merchant> searchNearby(double lng, double lat, double radiusKm);
}
