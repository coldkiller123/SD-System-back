package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.erp.dto.CustomerCreateRequest;
import org.example.erp.dto.CustomerUpdateRequest;
import org.example.erp.dto.CustomerListResponse;

import org.example.erp.entity.attachments;
import org.example.erp.entity.contacts;
import org.example.erp.entity.customers;
import org.example.erp.mapper.attachmentsMapper;
import org.example.erp.mapper.contactsMapper;
import org.example.erp.mapper.customersMapper;
import org.example.erp.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private customersMapper customersMapper;

    @Autowired
    private contactsMapper contactsMapper;

    @Autowired
    private attachmentsMapper attachmentsMapper; // 附件Mapper

    @Transactional
    @Override
    public void updateCustomer(String customerId, CustomerUpdateRequest request) {
        // 1. 查询原客户信息
        customers customer = customersMapper.selectById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在: " + customerId);
        }
        // 2. 更新客户基本字段（不变）
        customer.setName(request.getName());
        customer.setType(request.getType());
        customer.setRegion(request.getRegion());
        customer.setIndustry(request.getIndustry());
        customer.setCompany(request.getCompany());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setCreditRating(request.getCreditRating());
        customer.setRemarks(request.getRemarks());
        customersMapper.updateById(customer);

        // 3. 处理联系人：直接更新原有联系人，而非删除重建
        if (request.getContacts() != null && !request.getContacts().isEmpty()) {
            CustomerUpdateRequest.ContactDTO dto = request.getContacts().get(0);
            int contactId = customer.getContactId(); // 获取原联系人ID

            if (contactId != 0) { // 原联系人存在：更新信息
                contacts existingContact = contactsMapper.selectById(contactId);
                if (existingContact != null) {
                    existingContact.setName(dto.getName());
                    existingContact.setPhone(dto.getPhone());
                    existingContact.setEmail(dto.getEmail());
                    existingContact.setPosition(dto.getPosition());
                    contactsMapper.updateById(existingContact); // 直接更新
                } else {
                    // 极端情况：原联系人ID存在但记录已被删除，此时新增
                    contacts newContact = new contacts();
                    newContact.setName(dto.getName());
                    newContact.setPhone(dto.getPhone());
                    newContact.setEmail(dto.getEmail());
                    newContact.setPosition(dto.getPosition());
                    contactsMapper.insert(newContact);
                    customer.setContactId(newContact.getId());
                    customersMapper.updateById(customer);
                }
            } else { // 原联系人不存在：新增并关联
                contacts newContact = new contacts();
                newContact.setName(dto.getName());
                newContact.setPhone(dto.getPhone());
                newContact.setEmail(dto.getEmail());
                newContact.setPosition(dto.getPosition());
                contactsMapper.insert(newContact);
                customer.setContactId(newContact.getId());
                customersMapper.updateById(customer);
            }
        }

        // 4. 记录修改日志
        System.out.println("修改时间：" + request.getModifiedAt() + "，操作人：" + request.getModifiedBy());
    }

    // 创建客户方法
    @Transactional
    @Override
    public String createCustomer(CustomerCreateRequest request) {
        // 1. 生成客户唯一ID（不变）
        String customerId = "CUST_" + UUID.randomUUID().toString().substring(0, 8)
                + System.currentTimeMillis();

        // 2. 生成创建时间（不变）
        String createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 3. 处理联系人（删除mainContact，直接获取联系人ID）
        Integer contactId = null; // 直接用ID变量记录联系人ID
        if (request.getContacts() != null && !request.getContacts().isEmpty()) {
            // 取列表中第一个联系人（前端约定仅一个）
            CustomerCreateRequest.ContactDTO dto = request.getContacts().get(0);

            // 查询是否存在相同联系人
            contacts existingContact = contactsMapper.selectOne(
                    Wrappers.<contacts>lambdaQuery()
                            .eq(contacts::getName, dto.getName())
                            .eq(contacts::getPhone, dto.getPhone())
            );

            if (existingContact != null) {
                // 复用已有联系人，直接获取其ID
                contactId = existingContact.getId();
            } else {
                // 新增联系人，插入后获取ID
                contacts newContact = new contacts();
                newContact.setName(dto.getName());
                newContact.setPhone(dto.getPhone());
                newContact.setEmail(dto.getEmail());
                newContact.setPosition(dto.getPosition());
                contactsMapper.insert(newContact);
                contactId = newContact.getId(); // 直接获取新联系人ID
            }
        } else {
            throw new RuntimeException("联系人信息不能为空");
        }

        // 4. 保存客户基本信息（用contactId关联，不再依赖mainContact）
        customers customer = new customers();
        customer.setId(customerId);
        customer.setName(request.getName());
        customer.setType(request.getType());
        customer.setRegion(request.getRegion());
        customer.setIndustry(request.getIndustry());
        customer.setCompany(request.getCompany());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setCreditRating(request.getCreditRating());
        customer.setContactId(contactId); // 直接使用contactId变量
        customer.setRemarks(request.getRemarks());
        customer.setCreatedAt(createdAt);
        customersMapper.insert(customer);

        // 5. 处理附件（不变）
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (String filename : request.getAttachments()) {
                attachments attachment = new attachments();
                attachment.setCustomerId(customerId);
                attachment.setFileName(filename);
                attachment.setFilePath("/upload/customers/" + customerId + "/" + filename);
                attachmentsMapper.insert(attachment);
            }
        }
        return customerId;
    }

    @Override
    public CustomerListResponse getCustomerList(int pageIndex, Integer pageSize,
                                                String name, String region, String industry) {
        // 设置默认分页大小
        int defaultPageSize = 10;
        if (pageSize == null || pageSize <= 0) {
            pageSize = defaultPageSize;
        }

        // 构建分页对象（MyBatis-Plus的pageIndex从1开始，与需求一致）
        Page<customers> page = new Page<>(pageIndex, pageSize);

        // 构建查询条件
        QueryWrapper<customers> queryWrapper = Wrappers.query();

        // 按名称模糊搜索
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like("name", name.trim());
        }

        // 按地区筛选
        if (region != null && !region.trim().isEmpty()) {
            queryWrapper.eq("region", region.trim());
        }

        // 按行业筛选
        if (industry != null && !industry.trim().isEmpty()) {
            queryWrapper.eq("industry", industry.trim());
        }

        // 执行分页查询
        Page<customers> customerPage = customersMapper.selectPage(page, queryWrapper);

        // 计算总页数
        long total = customerPage.getTotal();
        int pageCount = (int) (total % pageSize == 0 ? total / pageSize : total / pageSize + 1);

        // 转换为响应DTO（使用修改后的内部类名CustomerListData）
        CustomerListResponse response = new CustomerListResponse();
        CustomerListResponse.CustomerListData data = new CustomerListResponse.CustomerListData();

        data.setTotal(total);
        data.setPageCount(pageCount);

        // 转换客户列表（关联查询联系人信息）
        List<CustomerListResponse.CustomerDTO> customerDTOs = customerPage.getRecords().stream().map(customer -> {
            CustomerListResponse.CustomerDTO dto = new CustomerListResponse.CustomerDTO();
            dto.setId(customer.getId());
            dto.setName(customer.getName());
            dto.setRegion(customer.getRegion());
            dto.setIndustry(customer.getIndustry());
            dto.setCompany(customer.getCompany());
            dto.setPhone(customer.getPhone());
            dto.setCreditRating(customer.getCreditRating());

            // 查询联系人姓名
            if (customer.getContactId() != 0) {
                contacts contact = contactsMapper.selectById(customer.getContactId());
                if (contact != null) {
                    dto.setContact(contact.getName());
                } else {
                    dto.setContact("未知联系人");
                }
            } else {
                dto.setContact("未设置联系人");
            }

            return dto;
        }).collect(Collectors.toList());

        data.setCustomers(customerDTOs);
        response.setData(data);

        return response;
    }
}

