package org.example.erp.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderPageResponseDTO {
    private int code = 200;
    private String message = "success";
    private OrderPageData data;

    @Data
    public static class OrderPageData {
        private long total; // 总记录数
        private int page; // 当前页码（从0开始）
        private int page_size; // 每页数量
        private List<OrderItemDTO> orders; // 订单列表
    }
}