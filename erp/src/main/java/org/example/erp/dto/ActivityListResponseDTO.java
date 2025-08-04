package org.example.erp.dto;

import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
public class ActivityListResponseDTO {
    private int code = 200;
    private String message = "success";
    private ActivityData data; // 包含活动列表

    @Data
    public static class ActivityData {
        private List<ActivityDTO> activities; // 活动列表
    }

    @Data
    public static class ActivityDTO {
        private String titleAct;       // 活动标题
        private String titleSta;       // 总数标题
        private Integer value;         // 总数
        private String icon;           // 前端组件
        private String todayNew;      // 今日新增
        private String description;    // 活动描述
        private String module;         // 所属模块
        private String color;          // 颜色标识
    }
}
