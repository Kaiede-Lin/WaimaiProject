package com.waimai.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.waimai.common.entity.DeliveryTrack;
import com.waimai.common.entity.Rider;

import java.util.List;

public interface RiderService extends IService<Rider> {

    Rider getByOpenid(String openid);

    void register(Rider rider);

    void auditRider(Long riderId, Integer auditStatus, String reason);

    void updateLocation(Long riderId, double lng, double lat);

    void goOnline(Long riderId);

    void goOffline(Long riderId);

    void acceptTask(Long riderId, Long orderId);

    void pickupTask(Long riderId, Long orderId);

    void completeTask(Long riderId, Long orderId);

    List<DeliveryTrack> getTracks(Long orderId);
}
