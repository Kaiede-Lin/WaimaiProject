package com.waimai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("delivery_sub_task")
public class DeliverySubTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long orderId;
    private Long riderId;
    private String subAddress;
    private BigDecimal subAddressLng;
    private BigDecimal subAddressLat;
    private String itemsJson;
    private String status;
    private Integer estimatedMinutes;
    private LocalDateTime deliverTime;
    private LocalDateTime completeTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
