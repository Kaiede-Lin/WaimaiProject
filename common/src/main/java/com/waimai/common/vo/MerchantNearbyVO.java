package com.waimai.common.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MerchantNearbyVO {
    private Long id;
    private String name;
    private String logo;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal score;
    private Integer monthlySales;
    private BigDecimal minDelivery;
    private BigDecimal deliveryFee;
    private Integer avgDeliveryTime;
    private Double distanceKm;
}
