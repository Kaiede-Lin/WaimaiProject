package com.waimai.common.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PlaceOrderDTO {
    private Long merchantId;
    private String address;
    private BigDecimal addressLng;
    private BigDecimal addressLat;
    private Long couponId;
    private String remark;
    private List<OrderItemDTO> items;
}
