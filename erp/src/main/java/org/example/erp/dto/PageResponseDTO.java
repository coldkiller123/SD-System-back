package org.example.erp.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResponseDTO {
    private int code;
    private String message;
    private PageData data;

    @Data
    public static class PageData {
        private long total;
        private int page;
        private int page_size;
        private List<OrderItemDTO> orders;
    }
}
