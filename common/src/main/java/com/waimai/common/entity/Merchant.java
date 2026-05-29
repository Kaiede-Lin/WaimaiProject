package com.waimai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String name;
    private String logo;
    private String banner;
    private String description;
    private String phone;
    private String businessLicense;
    private String rejectionReason;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String businessHours;
    private BigDecimal minDelivery;
    private BigDecimal deliveryFee;
    private Integer avgDeliveryTime;
    private Integer monthlySales;
    private BigDecimal score;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
