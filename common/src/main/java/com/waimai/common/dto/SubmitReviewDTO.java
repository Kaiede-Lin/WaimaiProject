package com.waimai.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReviewDTO {
    @NotNull
    private Long orderId;

    @Min(1) @Max(5)
    private Integer merchantRating;
    private String merchantContent;

    @Min(1) @Max(5)
    private Integer riderRating;
    private String riderContent;

    private String images;
}