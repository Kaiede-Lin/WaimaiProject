package com.waimai.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestRefundDTO {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "纠纷类型不能为空")
    private String type;

    @NotBlank(message = "退款原因不能为空")
    private String description;

    private String images;
}
