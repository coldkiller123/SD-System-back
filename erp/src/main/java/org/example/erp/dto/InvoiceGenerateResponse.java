package org.example.erp.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class InvoiceGenerateResponse {
    private String invoiceId;
    private String orderId;
    private String issueDate;
    private String dueDate;
    private BigDecimal taxRate;
    private String status;
    private Customer customer;
    private List<Item> items;

    @Data
    public static class Customer {
        private String id;
        private String name;
        private String address;
        private String taxId;
    }

    @Data
    public static class Item {
        private String id;
        private String name;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private String description;
    }
}
