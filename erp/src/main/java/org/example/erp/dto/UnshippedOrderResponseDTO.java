package org.example.erp.dto;

import java.math.BigDecimal;
import java.util.List;

public class UnshippedOrderResponseDTO {
    private int code;
    private String message;
    private Data data;

    // 内部类：封装分页数据
    public static class Data {
        private int total; // 总记录数
        private int page; // 当前页码
        private int page_size; // 每页数量
        private List<OrderItem> orders; // 订单列表

        // Getters and Setters
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getPage_size() { return page_size; }
        public void setPage_size(int page_size) { this.page_size = page_size; }
        public List<OrderItem> getOrders() { return orders; }
        public void setOrders(List<OrderItem> orders) { this.orders = orders; }
    }

    // 内部类：订单详情
    public static class OrderItem {
        private String id; // 订单ID
        private String customerId; // 客户ID
        private String customerName; // 客户名称
        private String productName; // 商品名称
        private BigDecimal quantity; // 数量
        private String orderDate; // 订单日期
        private BigDecimal amount; // 总金额
        private String status; // 状态（固定为“已付款”）

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }
}