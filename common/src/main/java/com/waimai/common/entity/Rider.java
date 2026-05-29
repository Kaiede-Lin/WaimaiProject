package com.waimai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rider")
public class Rider {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private Integer auditStatus;
    private String rejectionReason;
    private String realName;
    private String idCard;
    private String phone;
    private String avatar;
    private BigDecimal currentLng;
    private BigDecimal currentLat;
    private Integer status;
    private Integer totalOrders;
    private BigDecimal score;
    private BigDecimal balance;
    private String level;
    private Integer levelScore;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
