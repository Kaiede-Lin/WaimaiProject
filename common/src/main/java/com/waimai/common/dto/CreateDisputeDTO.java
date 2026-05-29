package com.waimai.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDisputeDTO {
    @NotNull
    private Long orderId;
    @NotBlank
    private String type;
    @NotBlank
    private String description;
    private String images;
}
