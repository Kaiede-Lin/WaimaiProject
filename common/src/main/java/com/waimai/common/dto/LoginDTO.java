package com.waimai.common.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class LoginDTO {
    @NotBlank(message = "code不能为空")
    private String code;
    private String nickname;
    private String avatar;
}
