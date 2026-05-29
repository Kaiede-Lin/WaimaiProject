package com.waimai.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewOrderDTO {
    private Long orderId;
    private String orderNo;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private String address;
    private String remark;
    private LocalDateTime createTime;
}
