package com.waimai.common.vo;

import lombok.Data;

@Data
public class RiderNearbyVO {
    private Long id;
    private String realName;
    private String phone;
    private Double distanceKm;
    private Integer currentLoad;
    private Double score;
}
