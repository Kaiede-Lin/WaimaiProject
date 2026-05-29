package com.waimai.service.impl;

import com.waimai.common.constant.OrderStatus;
import com.waimai.common.entity.Merchant;
import com.waimai.common.entity.Order;
import com.waimai.common.entity.Rider;
import com.waimai.common.vo.MerchantNearbyVO;
import com.waimai.common.vo.RiderNearbyVO;
import com.waimai.service.service.GeoService;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeoServiceImpl implements GeoService {

    private static final String MERCHANT_GEO_KEY = "waimai:geo:merchant";
    private static final String RIDER_GEO_KEY = "waimai:geo:rider";

    private final RedisTemplate<String, Object> redisTemplate;
    private final MerchantServiceImpl merchantService;
    private final RiderServiceImpl riderService;
    private final OrderServiceImpl orderService;

    public GeoServiceImpl(RedisTemplate<String, Object> redisTemplate,
                          MerchantServiceImpl merchantService,
                          RiderServiceImpl riderService,
                          OrderServiceImpl orderService) {
        this.redisTemplate = redisTemplate;
        this.merchantService = merchantService;
        this.riderService = riderService;
        this.orderService = orderService;
    }

    @Override
    public void addMerchantLocation(Long merchantId, double lng, double lat) {
        redisTemplate.opsForGeo().add(MERCHANT_GEO_KEY, new Point(lng, lat), merchantId.toString());
    }

    @Override
    public void addRiderLocation(Long riderId, double lng, double lat) {
        redisTemplate.opsForGeo().add(RIDER_GEO_KEY, new Point(lng, lat), riderId.toString());
    }

    @Override
    public void removeRiderLocation(Long riderId) {
        redisTemplate.opsForGeo().remove(RIDER_GEO_KEY, riderId.toString());
    }

    @Override
    public List<MerchantNearbyVO> searchNearbyMerchants(double lng, double lat, double radiusKm) {
        Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().includeCoordinates().sortAscending();

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisTemplate.opsForGeo().radius(MERCHANT_GEO_KEY, circle, args);

        List<MerchantNearbyVO> list = new ArrayList<>();

        // If Redis GeoHash returns nothing, fall back to MySQL-based search
        if (results == null || results.getContent().isEmpty()) {
            List<Merchant> fallback = merchantService.searchNearby(lng, lat, radiusKm);
            for (Merchant m : fallback) {
                MerchantNearbyVO vo = new MerchantNearbyVO();
                vo.setId(m.getId());
                vo.setName(m.getName());
                vo.setLogo(m.getLogo());
                vo.setAddress(m.getAddress());
                vo.setLongitude(m.getLongitude());
                vo.setLatitude(m.getLatitude());
                vo.setScore(m.getScore());
                vo.setMonthlySales(m.getMonthlySales());
                vo.setMinDelivery(m.getMinDelivery());
                vo.setDeliveryFee(m.getDeliveryFee());
                vo.setAvgDeliveryTime(m.getAvgDeliveryTime());
                double dLat = (m.getLatitude().doubleValue() - lat) * 111.0;
                double dLng = (m.getLongitude().doubleValue() - lng) * 111.0 * Math.cos(Math.toRadians(lat));
                vo.setDistanceKm(Math.round(Math.sqrt(dLat * dLat + dLng * dLng) * 100.0) / 100.0);
                list.add(vo);
            }
            return list;
        }

        List<Long> merchantIds = results.getContent().stream()
                .map(c -> Long.valueOf(String.valueOf(c.getContent().getName())))
                .collect(Collectors.toList());

        if (merchantIds.isEmpty()) return list;

        Map<Long, Merchant> merchantMap = merchantService.listByIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, m -> m));

        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results.getContent()) {
            Long id = Long.valueOf(String.valueOf(result.getContent().getName()));
            Merchant merchant = merchantMap.get(id);
            if (merchant == null || merchant.getStatus() != 1) continue;

            MerchantNearbyVO vo = new MerchantNearbyVO();
            vo.setId(id);
            vo.setName(merchant.getName());
            vo.setLogo(merchant.getLogo());
            vo.setAddress(merchant.getAddress());
            vo.setLongitude(merchant.getLongitude());
            vo.setLatitude(merchant.getLatitude());
            vo.setScore(merchant.getScore());
            vo.setMonthlySales(merchant.getMonthlySales());
            vo.setMinDelivery(merchant.getMinDelivery());
            vo.setDeliveryFee(merchant.getDeliveryFee());
            vo.setAvgDeliveryTime(merchant.getAvgDeliveryTime());
            vo.setDistanceKm(Math.round(result.getDistance().getValue() * 100.0) / 100.0);
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<RiderNearbyVO> searchNearbyRiders(double lng, double lat, double radiusKm) {
        Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().includeCoordinates().sortAscending();

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisTemplate.opsForGeo().radius(RIDER_GEO_KEY, circle, args);

        List<RiderNearbyVO> list = new ArrayList<>();
        if (results == null) return list;

        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results.getContent()) {
            Long riderId = Long.valueOf(String.valueOf(result.getContent().getName()));
            Rider rider = riderService.getById(riderId);
            if (rider == null || rider.getStatus() != 3) continue;

            int currentLoad = (int) orderService.lambdaQuery()
                    .eq(Order::getRiderId, riderId)
                    .eq(Order::getStatus, OrderStatus.DELIVERING)
                    .count().intValue();

            RiderNearbyVO vo = new RiderNearbyVO();
            vo.setId(riderId);
            vo.setRealName(rider.getRealName());
            vo.setPhone(rider.getPhone());
            vo.setDistanceKm(Math.round(result.getDistance().getValue() * 100.0) / 100.0);
            vo.setCurrentLoad(currentLoad);
            vo.setScore(rider.getScore() != null ? rider.getScore().doubleValue() : 5.0);
            list.add(vo);
        }
        return list;
    }
}
