package com.waimai.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportExceptionDTO {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "异常类型不能为空")
    private String type;

    @NotBlank(message = "异常描述不能为空")
    private String description;

    private String images;
}
