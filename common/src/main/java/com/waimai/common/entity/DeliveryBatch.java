package com.waimai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("delivery_batch")
public class DeliveryBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String batchNo;
    private Integer totalSubCount;
    private Integer completedSubCount;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
