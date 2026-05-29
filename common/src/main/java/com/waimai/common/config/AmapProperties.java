package com.waimai.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.map")
public class AmapProperties {
    private String provider;
    private String apiKey;
    private String baseUrl = "https://restapi.amap.com";
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}
