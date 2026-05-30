package com.waimai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("delivery_exception")
public class DeliveryException {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long riderId;
    private String type;
    private String description;
    private String images;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
