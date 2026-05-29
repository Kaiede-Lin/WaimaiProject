package com.waimai.api.config;

import com.waimai.common.config.AmapProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private final AmapProperties amapProperties;

    public RestTemplateConfig(AmapProperties amapProperties) {
        this.amapProperties = amapProperties;
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(amapProperties.getConnectTimeout());
        factory.setReadTimeout(amapProperties.getReadTimeout());
        return new RestTemplate(factory);
    }
}
