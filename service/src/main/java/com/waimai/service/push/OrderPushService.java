package com.waimai.service.push;

import com.waimai.common.dto.NewOrderDTO;
import com.waimai.common.dto.RiderLocationDTO;
import com.waimai.common.dto.WsMessage;
import com.waimai.common.entity.Order;
import com.waimai.common.entity.OrderDetail;
import com.waimai.common.entity.Rider;
import com.waimai.common.websocket.WebSocketServer;
import com.waimai.service.impl.OrderServiceImpl;
import com.waimai.service.impl.RiderServiceImpl;
import com.waimai.service.mapper.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class OrderPushService {

    private final OrderServiceImpl orderService;
    private final RiderServiceImpl riderService;
    private final OrderDetailMapper orderDetailMapper;

    public OrderPushService(OrderServiceImpl orderService, RiderServiceImpl riderService,
                            OrderDetailMapper orderDetailMapper) {
        this.orderService = orderService;
        this.riderService = riderService;
        this.orderDetailMapper = orderDetailMapper;
    }

    /**
     * Push new order notification to merchant after payment success.
     */
    public void pushNewOrderToMerchant(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) return;

        NewOrderDTO dto = new NewOrderDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayAmount(order.getPayAmount());
        dto.setAddress(order.getAddress());
        dto.setRemark(order.getRemark());
        dto.setCreateTime(order.getCreateTime());

        WsMessage msg = new WsMessage("NEW_ORDER", dto);
        WebSocketServer.sendToMerchant(order.getMerchantId(), msg);
        log.info("推送新订单给商家: merchantId={}, orderNo={}", order.getMerchantId(), order.getOrderNo());
    }

    /**
     * Push rider location update to the subscribed user.
     */
    public void pushRiderLocationToUser(Long riderId, double lng, double lat) {
        Rider rider = riderService.getById(riderId);
        if (rider == null) return;

        // Find the rider's currently delivering order
        Order activeOrder = orderService.lambdaQuery()
                .eq(Order::getRiderId, riderId)
                .eq(Order::getStatus, "DELIVERING")
                .orderByDesc(Order::getCreateTime)
                .one();

        if (activeOrder == null) return;

        RiderLocationDTO dto = new RiderLocationDTO();
        dto.setRiderId(riderId);
        dto.setRiderName(rider.getRealName());
        dto.setRiderPhone(rider.getPhone());
        dto.setLongitude(lng);
        dto.setLatitude(lat);
        dto.setOrderId(activeOrder.getId());
        dto.setOrderNo(activeOrder.getOrderNo());

        WsMessage msg = new WsMessage("RIDER_LOCATION", dto);
        WebSocketServer.sendToUser(activeOrder.getUserId(), msg);

        // Also push to merchant
        MerchantLocationPush dto2 = new MerchantLocationPush();
        dto2.setRiderId(riderId);
        dto2.setRiderName(rider.getRealName());
        dto2.setLongitude(lng);
        dto2.setLatitude(lat);
        dto2.setOrderId(activeOrder.getId());
        dto2.setOrderNo(activeOrder.getOrderNo());
        WsMessage msg2 = new WsMessage("RIDER_LOCATION", dto2);
        WebSocketServer.sendToMerchant(activeOrder.getMerchantId(), msg2);
    }

    /**
     * Push dispatch offer to the best-scored rider.
     */
    public void pushDispatchToRider(Long riderId, Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) return;

        WsMessage msg = new WsMessage("NEW_TASK", order);
        WebSocketServer.sendToRider(riderId, msg);
        log.info("派单推送: riderId={}, orderNo={}", riderId, order.getOrderNo());
    }

    @lombok.Data
    public static class MerchantLocationPush {
        private Long riderId;
        private String riderName;
        private double longitude;
        private double latitude;
        private Long orderId;
        private String orderNo;
    }
}
