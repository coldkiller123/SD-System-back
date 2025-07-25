package org.example.erp.dto;

public class InquiryStatusUpdateDTO {
    private String status; // 新状态（未报价/已报价）
    private String user; // 操作人

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}