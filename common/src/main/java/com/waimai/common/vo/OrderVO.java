package com.waimai.common.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String address;
    private String remark;
    private String merchantName;
    private String riderName;
    private String riderPhone;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private List<OrderDetailVO> details;
}
