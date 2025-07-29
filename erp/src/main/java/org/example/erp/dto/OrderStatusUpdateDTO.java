package org.example.erp.dto;

import lombok.Data;

@Data
public class OrderStatusUpdateDTO {
    private String status; // 新状态（仅接受"已完成"）
}