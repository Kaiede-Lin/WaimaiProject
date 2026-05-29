package com.waimai.service.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.waimai.common.dto.OrderItemDTO;
import com.waimai.common.entity.Order;

import java.util.List;

public interface OrderService extends IService<Order> {

    Order placeOrder(Long userId, Long merchantId, String address,
                     double addressLng, double addressLat, String remark,
                     List<OrderItemDTO> items, Long couponId);

    Order payOrder(Long orderId);

    void cancelOrder(Long orderId, Long userId);

    Order getByOrderNo(String orderNo);

    Page<Order> listByUser(Long userId, int page, int size);

    Page<Order> listByMerchant(Long merchantId, int page, int size);

    void acceptOrder(Long orderId, Long merchantId);
}
