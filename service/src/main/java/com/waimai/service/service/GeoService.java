package com.waimai.service.service;

import com.waimai.common.vo.MerchantNearbyVO;
import com.waimai.common.vo.RiderNearbyVO;

import java.util.List;

public interface GeoService {

    void addMerchantLocation(Long merchantId, double lng, double lat);

    void addRiderLocation(Long riderId, double lng, double lat);

    void removeRiderLocation(Long riderId);

    List<MerchantNearbyVO> searchNearbyMerchants(double lng, double lat, double radiusKm);

    List<RiderNearbyVO> searchNearbyRiders(double lng, double lat, double radiusKm);
}
