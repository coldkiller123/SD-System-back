package org.example.erp.controller;

import org.example.erp.dto.CustomerCreateRequest;
import org.example.erp.dto.CustomerUpdateRequest;
import org.example.erp.dto.CustomerListResponse;
import org.example.erp.dto.FileUploadResponse;
import org.example.erp.dto.CustomerDetailResponse;

import org.example.erp.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/customer")
public class customersController {

    @Autowired
    private CustomerService customerService;

    // 已有更新接口
    @PutMapping("/update")
    public Map<String, Object> updateCustomer(@RequestParam("id") String customerId,
                                              @RequestBody CustomerUpdateRequest request) {
        customerService.updateCustomer(customerId, request);
        Map<String, Object> resp = new HashMap<>();
        resp.put("info", "客户信息修改成功");
        return resp;
    }

    // 新增创建客户接口
    @PostMapping("/create")
    public Map<String, Object> createCustomer(@RequestBody CustomerCreateRequest request) {
        // 调用服务层创建客户，获取生成的客户ID
        String customerId = customerService.createCustomer(request);
        // 返回成功信息（按需求，前端显示客户ID）
        Map<String, Object> resp = new HashMap<>();
        resp.put("info", "客户信息创建成功");
        resp.put("customerId", customerId); // 返回生成的客户ID
        return resp;
    }

    //分页查看客户信息
    @GetMapping("/list")
    public CustomerListResponse getCustomerList(
            @RequestParam(required = true) int pageIndex,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String industry) {

        return customerService.getCustomerList(pageIndex, pageSize, name, region, industry);
    }

    //查询客户信息详情
    @GetMapping("/detail")
    public CustomerDetailResponse getCustomerDetail(@RequestParam("id") String customerId) {
        return customerService.getCustomerDetail(customerId);
    }

    //客户附件上传接口
    @PostMapping("/upload")
    public FileUploadResponse uploadFiles(
            @RequestParam("id") String customerId,
            @RequestParam("file") MultipartFile[] files) {
        return customerService.uploadAttachments(customerId, files);
    }
}
