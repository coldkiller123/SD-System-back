package org.example.erp.service;

import org.example.erp.dto.CustomerCreateRequest;
import org.example.erp.dto.CustomerListResponse;
import org.example.erp.dto.CustomerUpdateRequest;

public interface CustomerService {
    //更新客户方法
    void updateCustomer(String customerId, CustomerUpdateRequest request);

    // 创建客户方法：返回生成的客户ID
    String createCustomer(CustomerCreateRequest request);

    // 分页查询客户列表
    CustomerListResponse getCustomerList(int pageIndex, Integer pageSize,
                                         String name, String region, String industry);
}
