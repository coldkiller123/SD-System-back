package org.example.erp.dto;

import lombok.Data;
import java.util.List;

@Data
public class CustomerListResponse {
    private CustomerListData data; // 引用修改后的内部类

    // 内部类改名：Data → CustomerListData
    @Data
    public static class CustomerListData {
        private long total;          // 总记录数
        private int pageCount;       // 总页数
        private List<CustomerDTO> customers;  // 当前页客户列表
    }

    @Data
    public static class CustomerDTO {
        private String id;
        private String name;
        private String region;
        private String industry;
        private String company;
        private String contact;      // 联系人姓名
        private String phone;        // 客户电话
        private String creditRating; // 信用评级
    }
}