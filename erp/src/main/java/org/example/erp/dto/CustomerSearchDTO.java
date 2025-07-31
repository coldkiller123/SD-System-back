package org.example.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// 只包含前端需要的客户ID和姓名字段
@Data
@AllArgsConstructor  // 生成带参构造器，便于快速创建对象
public class CustomerSearchDTO {
    private String id;    // 客户ID
    private String name;  // 客户姓名
}
