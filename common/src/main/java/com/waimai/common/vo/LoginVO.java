package com.waimai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String nickname;
    private String avatar;
}
