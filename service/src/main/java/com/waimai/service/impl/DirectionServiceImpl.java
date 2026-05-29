package com.waimai.service.impl;

import com.waimai.common.config.AmapProperties;
import com.waimai.service.service.DirectionService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DirectionServiceImpl implements DirectionService {

    private final AmapProperties amapProperties;
    private final RestTemplate restTemplate;

    public DirectionServiceImpl(AmapProperties amapProperties, RestTemplate restTemplate) {
        this.amapProperties = amapProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getDrivingRoute(String origin, String destination) {
        String url = amapProperties.getBaseUrl() + "/v3/direction/driving"
                + "?key=" + amapProperties.getApiKey()
                + "&origin=" + origin
                + "&destination=" + destination
                + "&strategy=0"
                + "&extensions=all"
                + "&output=JSON";
        return restTemplate.getForObject(url, String.class);
    }
}
