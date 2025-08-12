package org.example.erp.service;

import org.example.erp.dto.CustomerCreateRequest;
import org.example.erp.dto.CustomerListResponse;
import org.example.erp.dto.CustomerUpdateRequest;
import org.example.erp.dto.FileUploadResponse;
import org.example.erp.dto.CustomerDetailResponse;
import org.example.erp.dto.CustomerSearchDTO;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface CustomerService {
    //更新客户方法
    void updateCustomer(String customerId, CustomerUpdateRequest request);

    // 创建客户方法：返回生成的客户ID
    String createCustomer(CustomerCreateRequest request);

    // 分页查询客户列表
    CustomerListResponse getCustomerList(int pageIndex, Integer pageSize,
                                         String name, String region, String industry, String creditRating);

    //上传客户附件
    FileUploadResponse uploadAttachments(String customerId, MultipartFile[] files);

    //查询客户详细信息
    CustomerDetailResponse getCustomerDetail(String customerId);

    // 根据关键词搜索客户（匹配ID或姓名）
    List<CustomerSearchDTO> searchCustomers(String keyword);

}
