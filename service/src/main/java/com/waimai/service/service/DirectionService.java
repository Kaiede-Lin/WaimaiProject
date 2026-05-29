package com.waimai.service.service;

public interface DirectionService {
    /**
     * Call Amap v3/direction/driving API and return the raw JSON response.
     * @param origin  "lng,lat" format
     * @param destination  "lng,lat" format
     * @return raw JSON string from Amap
     */
    String getDrivingRoute(String origin, String destination);
}
