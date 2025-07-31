package org.example.erp.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDTO {
    private String id;
    private String name;
    private BigDecimal price;  // 对应实体类的unitPrice
    private BigDecimal stock;  // 对应实体类的quantity
    private String description;
}
