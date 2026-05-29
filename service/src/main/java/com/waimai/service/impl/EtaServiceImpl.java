package com.waimai.service.impl;

import com.waimai.common.entity.Merchant;
import com.waimai.common.entity.Order;
import com.waimai.common.entity.Rider;
import com.waimai.service.service.EtaService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class EtaServiceImpl implements EtaService {

    private static final double AVG_SPEED_KMH = 20.0; // Average delivery speed km/h
    private static final double TRAFFIC_FACTOR = 1.2;

    private final OrderServiceImpl orderService;
    private final MerchantServiceImpl merchantService;
    private final RiderServiceImpl riderService;

    public EtaServiceImpl(OrderServiceImpl orderService,
                          MerchantServiceImpl merchantService,
                          @Lazy RiderServiceImpl riderService) {
        this.orderService = orderService;
        this.merchantService = merchantService;
        this.riderService = riderService;
    }

    @Override
    public int calculateEta(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) return 30;

        double fromLng, fromLat;
        Rider rider = order.getRiderId() != null ? riderService.getById(order.getRiderId()) : null;
        if (rider != null && rider.getCurrentLng() != null && rider.getCurrentLat() != null) {
            fromLng = rider.getCurrentLng().doubleValue();
            fromLat = rider.getCurrentLat().doubleValue();
        } else {
            Merchant merchant = merchantService.getById(order.getMerchantId());
            if (merchant == null || merchant.getLongitude() == null || merchant.getLatitude() == null) {
                return 30;
            }
            fromLng = merchant.getLongitude().doubleValue();
            fromLat = merchant.getLatitude().doubleValue();
        }

        double toLng = order.getAddressLng() != null ? order.getAddressLng().doubleValue() : 0;
        double toLat = order.getAddressLat() != null ? order.getAddressLat().doubleValue() : 0;

        double distanceKm = haversine(fromLat, fromLng, toLat, toLng);
        int travelMinutes = (int) Math.ceil((distanceKm / AVG_SPEED_KMH) * 60 * TRAFFIC_FACTOR);

        Merchant merchant = merchantService.getById(order.getMerchantId());
        int prepMinutes = merchant != null && merchant.getAvgDeliveryTime() != null
                ? merchant.getAvgDeliveryTime() : 20;

        return travelMinutes + prepMinutes;
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
