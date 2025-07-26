package org.example.erp.dto;

import java.util.List;

public class CustomerCreateRequest {
    private String name; // 客户名称（必填）
    private String type; // 客户类型（必填）
    private String region; // 所在地区（必填）
    private String industry; // 所属行业（必填）
    private String company; // 所属公司（必填）
    private String phone; // 联系电话（必填）
    private String address; // 详细地址（必填）
    private String creditRating; // 信用等级（必填）
    private List<ContactDTO> contacts; // 联系人列表（必填）
    private String remarks; // 备注（可选）
    private List<String> attachments; // 附件（可选）
    private String createdAt; // 创建时间（后端自动生成，前端可不传）

    // 内部类：联系人DTO（复用已有结构，与更新接口保持一致）
    public static class ContactDTO {
        private String name;
        private String position;
        private String phone;
        private String email;

        // getter 和 setter
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // 外部类 getter 和 setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCreditRating() { return creditRating; }
    public void setCreditRating(String creditRating) { this.creditRating = creditRating; }
    public List<ContactDTO> getContacts() { return contacts; }
    public void setContacts(List<ContactDTO> contacts) { this.contacts = contacts; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}