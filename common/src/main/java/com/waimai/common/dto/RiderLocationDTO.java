package com.waimai.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiderLocationDTO {
    private Long riderId;
    private String riderName;
    private String riderPhone;
    private double longitude;
    private double latitude;
    private Long orderId;
    private String orderNo;
}
