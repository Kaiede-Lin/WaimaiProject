package com.waimai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rider_income")
public class RiderIncome {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long riderId;
    private Long orderId;
    private BigDecimal amount;
    private String type;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}