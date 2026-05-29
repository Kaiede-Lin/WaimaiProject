package com.waimai.service.service;

import com.waimai.common.entity.Order;

public interface DispatchService {

    /**
     * Smart dispatch: score nearby riders by distance + workload,
     * select the best one, and push dispatch notification via WebSocket.
     */
    Long dispatchOrder(Long orderId);

    void autoDispatchForMerchant(Long merchantId);
}
