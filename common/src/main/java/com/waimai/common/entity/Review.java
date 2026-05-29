package com.waimai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long userId;
    private Long merchantId;
    private Long riderId;
    private Integer rating;
    private String content;
    private String images;
    private String type;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
