package org.example.erp.dto;

import java.math.BigDecimal;

public class OrderUpdateDTO {
    private String customerId;
    private String productName;
    private String productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String status;
    private String salesPerson;
    private String remarks;
    // 不可编辑字段：createdAt不接收，由数据库保留原始值
    private String modifiedAt; // 自动生成，前端可传但会被覆盖
    private String modifiedBy; // 必须传修改人

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSalesPerson() { return salesPerson; }
    public void setSalesPerson(String salesPerson) { this.salesPerson = salesPerson; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(String modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}