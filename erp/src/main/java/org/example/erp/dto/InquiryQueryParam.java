package org.example.erp.dto;

public class InquiryQueryParam {
    private Integer pageIndex = 0; // 默认第0页
    private Integer pageSize = 10; // 默认每页10条
    private String inquiryId; // 询价单号模糊搜索
    private String customerName; // 客户名称模糊搜索
    private String status; // 状态筛选（未报价/已报价）

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

    public String getInquiryId() {
        return inquiryId;
    }

    public void setInquiryId(String inquiryId) {
        this.inquiryId = inquiryId;
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