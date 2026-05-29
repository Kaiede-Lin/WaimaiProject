package com.waimai.common.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class MerchantReportVO {
    private BigDecimal totalRevenue;
    private Long orderCount;
    private BigDecimal avgOrderValue;
    private List<Map<String, Object>> topDishes;
}
