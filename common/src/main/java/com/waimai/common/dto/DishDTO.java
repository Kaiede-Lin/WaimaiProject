package com.waimai.common.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class DishDTO {
    @NotBlank(message = "菜品名称不能为空")
    private String name;
    private String image;
    @NotNull(message = "价格不能为空")
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String richDescription;
    private String summary;
    private Integer stock;
    private Integer sort;
    private Long categoryId;
}
