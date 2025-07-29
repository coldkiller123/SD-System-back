package org.example.erp.dto;

import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
public class ActivityListResponseDTO {
    private int code = 200;
    private String message = "success";
    private ActivityData data; // 包含活动列表和总数

    @Data
    public static class ActivityData {
        private List<ActivityDTO> activities; // 活动列表
        private Totals totals; // 各实体总数
    }

    @Data
    public static class Totals {
        private long orderCount;      // 订单总数
        private long customerCount;   // 客户总数
        private long deliveryCount;   // 发货单总数
        private long invoiceCount;    // 发票总数
    }

    @Data
    public static class ActivityDTO {
        private String title;
        private String description;
        private String module;
        private String color;
    }
}