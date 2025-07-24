package org.example.erp.dto;

public class OrderQueryParam {
    private Integer pageIndex = 0; // 当前页码（从0开始）
    private Integer pageSize = 10; // 每页数量，默认10
    private String orderId;       // 按订单编号模糊搜索
    private String customerName;  // 按客户名称模糊搜索
    private String status;        // 订单状态

    // Getters and Setters
    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}