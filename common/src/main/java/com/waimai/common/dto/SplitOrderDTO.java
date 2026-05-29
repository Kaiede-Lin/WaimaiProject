package com.waimai.common.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SplitOrderDTO {
    private Long orderId;
    private List<SubTaskInfo> subTasks;

    @Data
    public static class SubTaskInfo {
        private String address;
        private BigDecimal addressLng;
        private BigDecimal addressLat;
        private String itemsJson;
    }
}
