package com.waimai.common.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewVO {
    private Long id;
    private Long orderId;
    private String userName;
    private String userAvatar;
    private Integer rating;
    private String content;
    private String images;
    private String type;
    private LocalDateTime createTime;
}