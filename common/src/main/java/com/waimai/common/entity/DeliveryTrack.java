package com.waimai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("delivery_track")
public class DeliveryTrack {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long riderId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
