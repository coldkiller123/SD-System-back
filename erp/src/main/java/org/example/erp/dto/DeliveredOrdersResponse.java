package org.example.erp.dto;
import lombok.Data;
import java.util.List;

@Data
public class DeliveredOrdersResponse {
    private List<OrderItem> data; // 当前页订单列表
    private long total; // 筛选后的总记录数
    private int pageCount; // 总页数

    @Data
    public static class OrderItem {
        private String id; // 订单编号
        private String customerId; // 客户 ID
        private String customerName; // 客户名称
        private Number amount; // 订单金额
        private String orderDate; // 订单日期（ISO 格式）
        private String deliveryDate; // 收货日期（ISO 格式）
        private String status; // 订单状态（固定为 "已完成"）
        private boolean hasInvoice; // 是否已开票
        private String invoiceId; // 发票 ID（null 表示未开票）
    }
}
