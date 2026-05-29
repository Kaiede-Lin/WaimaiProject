package com.waimai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waimai.common.config.AmapProperties;
import com.waimai.common.constant.OrderStatus;
import com.waimai.common.entity.Merchant;
import com.waimai.common.entity.Order;
import com.waimai.common.entity.Rider;
import com.waimai.common.utils.MapUtils;
import com.waimai.common.vo.MerchantNearbyVO;
import com.waimai.common.vo.RiderNearbyVO;
import com.waimai.service.service.GeoService;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GeoServiceImpl implements GeoService {

    private static final String MERCHANT_GEO_KEY = "waimai:geo:merchant";
    private static final String RIDER_GEO_KEY = "waimai:geo:rider";
    private static final String AMAP_CACHE_PREFIX = "waimai:amap:nearby:";
    private static final int CACHE_TTL_MINUTES = 10;
    private static final String AMAP_AROUND_PATH = "/v3/place/around";

    private final RedisTemplate<String, Object> redisTemplate;
    private final MerchantServiceImpl merchantService;
    private final RiderServiceImpl riderService;
    private final OrderServiceImpl orderService;
    private final RestTemplate restTemplate;
    private final AmapProperties amapProperties;
    private final ObjectMapper objectMapper;

    public GeoServiceImpl(RedisTemplate<String, Object> redisTemplate,
                          MerchantServiceImpl merchantService,
                          RiderServiceImpl riderService,
                          OrderServiceImpl orderService,
                          RestTemplate restTemplate,
                          AmapProperties amapProperties) {
        this.redisTemplate = redisTemplate;
        this.merchantService = merchantService;
        this.riderService = riderService;
        this.orderService = orderService;
        this.restTemplate = restTemplate;
        this.amapProperties = amapProperties;
        this.objectMapper = new ObjectMapper();
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
        // Build grid-based cache key to group nearby coordinates
        String gridKey = MapUtils.buildGridKey(lat, lng);
        String cacheKey = AMAP_CACHE_PREFIX + gridKey + ":" + Math.round(radiusKm * 1000);

        // Check Redis cache first
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return toMerchantNearbyVOList(cached);
        }

        // Cache miss — call Amap Around Search API, always merge with local merchants
        List<MerchantNearbyVO> results;
        try {
            results = callAmapAroundSearch(lng, lat, radiusKm);
            // Merge with local DB merchants so every result has a valid ID
            mergeLocalMerchants(results, lng, lat, radiusKm);
        } catch (Exception e) {
            results = fallbackToMysqlSearch(lng, lat, radiusKm);
        }

        // Async write to Redis cache
        final List<MerchantNearbyVO> cacheResults = results;
        CompletableFuture.runAsync(() ->
                redisTemplate.opsForValue().set(cacheKey, cacheResults, CACHE_TTL_MINUTES, TimeUnit.MINUTES));

        return results;
    }

    @Override
    public void evictNearbyCache() {
        try {
            var keys = redisTemplate.keys(AMAP_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
        }
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

    private List<MerchantNearbyVO> callAmapAroundSearch(double lng, double lat, double radiusKm) {
        // Convert WGS-84 to GCJ-02 (Amap uses GCJ-02)
        double[] gcj02 = MapUtils.wgs84ToGcj02(lng, lat);

        String url = amapProperties.getBaseUrl() + AMAP_AROUND_PATH
                + "?key=" + amapProperties.getApiKey()
                + "&location=" + gcj02[0] + "," + gcj02[1]
                + "&radius=" + Math.round(radiusKm * 1000)
                + "&types=050000"
                + "&offset=25"
                + "&extensions=all";

        String response = restTemplate.getForObject(url, String.class);
        return parseAmapResponse(response);
    }

    private List<MerchantNearbyVO> parseAmapResponse(String response) {
        List<MerchantNearbyVO> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.get("status").asText())) {
                return results;
            }
            JsonNode pois = root.get("pois");
            if (pois == null || !pois.isArray() || pois.isEmpty()) {
                return results;
            }

            // Collect POI names for batch matching against local merchants
            List<String> poiNames = new ArrayList<>();
            for (JsonNode poi : pois) {
                if (poi.has("name")) {
                    poiNames.add(poi.get("name").asText());
                }
            }

            // Batch query local merchants by name
            Map<String, Merchant> merchantByName = merchantService.lambdaQuery()
                    .eq(Merchant::getStatus, 1)
                    .in(Merchant::getName, poiNames)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Merchant::getName, m -> m, (a, b) -> a));

            for (JsonNode poi : pois) {
                String name = poi.has("name") ? poi.get("name").asText() : "";
                Merchant matched = merchantByName.get(name);

                MerchantNearbyVO vo = new MerchantNearbyVO();
                if (matched != null) {
                    vo.setId(matched.getId());
                    vo.setName(matched.getName());
                    vo.setLogo(matched.getLogo());
                    vo.setAddress(matched.getAddress());
                    vo.setLongitude(matched.getLongitude());
                    vo.setLatitude(matched.getLatitude());
                    vo.setScore(matched.getScore());
                    vo.setMonthlySales(matched.getMonthlySales());
                    vo.setMinDelivery(matched.getMinDelivery());
                    vo.setDeliveryFee(matched.getDeliveryFee());
                    vo.setAvgDeliveryTime(matched.getAvgDeliveryTime());
                } else {
                    vo.setName(name);
                    vo.setAddress(poi.has("address") ? poi.get("address").asText() : "");
                    String[] loc = poi.get("location").asText().split(",");
                    vo.setLongitude(new BigDecimal(loc[0]));
                    vo.setLatitude(new BigDecimal(loc[1]));
                }

                double distanceMeters = poi.get("distance").asDouble();
                vo.setDistanceKm(Math.round(distanceMeters / 10.0) / 100.0);
                results.add(vo);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Amap response", e);
        }
        return results;
    }

    /**
     * Merge local DB merchants into Amap results so every entry has a valid ID.
     * Amap-matched merchants keep their Amap distance; unmatched locals use haversine.
     */
    private void mergeLocalMerchants(List<MerchantNearbyVO> results, double lng, double lat, double radiusKm) {
        Set<Long> existingIds = results.stream()
                .map(MerchantNearbyVO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<Merchant> locals = merchantService.searchNearby(lng, lat, radiusKm);
        for (Merchant m : locals) {
            if (existingIds.contains(m.getId())) continue;

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
            vo.setDistanceKm(Math.round(MapUtils.haversineKm(lng, lat,
                    m.getLongitude().doubleValue(), m.getLatitude().doubleValue()) * 100.0) / 100.0);
            results.add(vo);
        }
    }

    private List<MerchantNearbyVO> fallbackToMysqlSearch(double lng, double lat, double radiusKm) {
        List<MerchantNearbyVO> list = new ArrayList<>();
        List<Merchant> merchants = merchantService.searchNearby(lng, lat, radiusKm);
        for (Merchant m : merchants) {
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
            vo.setDistanceKm(Math.round(MapUtils.haversineKm(lng, lat,
                    m.getLongitude().doubleValue(), m.getLatitude().doubleValue()) * 100.0) / 100.0);
            list.add(vo);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<MerchantNearbyVO> toMerchantNearbyVOList(Object cached) {
        if (cached instanceof List) {
            List<?> list = (List<?>) cached;
            List<MerchantNearbyVO> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof MerchantNearbyVO vo) {
                    result.add(vo);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return new ArrayList<>();
    }
}
