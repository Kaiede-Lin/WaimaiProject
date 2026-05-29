package com.waimai.common.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderDetailVO {
    private Long dishId;
    private String dishName;
    private String dishImage;
    private BigDecimal price;
    private Integer quantity;
}
