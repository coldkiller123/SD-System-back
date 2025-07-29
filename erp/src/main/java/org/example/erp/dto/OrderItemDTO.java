package org.example.erp.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private String id; // 订单号（orders.id）
    private String deliveryOrderId; // 发货单号（delivery_orders.id 的字符串形式）
    private String customerName; // 客户名称
    private String productName; // 商品名称
    private BigDecimal quantity; // 数量
    private BigDecimal amount; // 订单金额（orders.totalAmount）
    private String orderDate; // 下单时间（orders.createdAt）
    private String status; // 订单状态
}