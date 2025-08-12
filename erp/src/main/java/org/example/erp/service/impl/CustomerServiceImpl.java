package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.erp.dto.CustomerCreateRequest;
import org.example.erp.dto.CustomerUpdateRequest;
import org.example.erp.dto.CustomerListResponse;
import org.example.erp.dto.FileUploadResponse;
import org.example.erp.dto.CustomerDetailResponse;
import org.example.erp.dto.CustomerSearchDTO;

import org.example.erp.entity.attachments;
import org.example.erp.entity.contacts;
import org.example.erp.entity.customers;
import org.example.erp.mapper.attachmentsMapper;
import org.example.erp.mapper.contactsMapper;
import org.example.erp.mapper.customersMapper;
import org.example.erp.service.ActivityService;
import org.example.erp.service.CustomerService;
import org.example.erp.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private customersMapper customersMapper;

    @Autowired
    private contactsMapper contactsMapper;

    @Autowired
    private attachmentsMapper attachmentsMapper;

    // 从配置文件读取上传根路径，建议在application.properties中配置
    @Value("${file.upload.base-path:/uploads}")
    private String baseUploadPath;

    //查询所有满足搜索条件客户
    @Override
    public List<CustomerSearchDTO> searchCustomers(String keyword) {
        // 1. 构建查询条件
        QueryWrapper<customers> queryWrapper = new QueryWrapper<>();

        // 2. 关键词不为空时，添加模糊查询条件（匹配ID或姓名）
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimKeyword = keyword.trim();
            queryWrapper.and(wrapper ->
                    wrapper.like("id", trimKeyword)  // 匹配客户ID
                            .or()
                            .like("name", trimKeyword)  // 匹配客户姓名
            );
        }

        // 3. 执行查询（无关键词时返回所有客户）
        List<customers> customerList = customersMapper.selectList(queryWrapper);

        // 4. 转换为DTO（只返回ID和姓名，避免暴露多余字段）
        return customerList.stream()
                .map(customer -> new CustomerSearchDTO(customer.getId(), customer.getName()))
                .collect(Collectors.toList());
    }

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
        customer.setModifiedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        customer.setModifiedBy(request.getModifiedBy()); // 本身是字符串，直接赋值
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
        // 1. 验证地区代码有效性
        String regionCode = getRegionCode(request.getRegion());
        if (regionCode == null) {
            throw new RuntimeException("无效的地区信息: " + request.getRegion());
        }

        // 2. 获取当前地区的最大流水号
        int maxSerialNumber = getMaxSerialNumberByRegion(regionCode);

        // 3. 生成5位流水号（自动递增）
        String serialNumber = String.format("%05d", maxSerialNumber + 1);

        // 4. 组合客户ID（CUS + 地区代码 + 5位流水号）
        String customerId = "CUS" + regionCode + serialNumber;

        // 5. 生成创建时间
        String createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 6. 处理联系人
        Integer contactId = null;
        if (request.getContacts() != null && !request.getContacts().isEmpty()) {
            CustomerCreateRequest.ContactDTO dto = request.getContacts().get(0);

            // 查询是否存在相同联系人
            contacts existingContact = contactsMapper.selectOne(
                    Wrappers.<contacts>lambdaQuery()
                            .eq(contacts::getName, dto.getName())
                            .eq(contacts::getPhone, dto.getPhone())
            );

            if (existingContact != null) {
                contactId = existingContact.getId();
            } else {
                contacts newContact = new contacts();
                newContact.setName(dto.getName());
                newContact.setPhone(dto.getPhone());
                newContact.setEmail(dto.getEmail());
                newContact.setPosition(dto.getPosition());
                contactsMapper.insert(newContact);
                contactId = newContact.getId();
            }
        } else {
            throw new RuntimeException("联系人信息不能为空");
        }

        // 7. 保存客户基本信息
        customers customer = new customers();
        customer.setId(customerId);
        customer.setName(request.getName());
        customer.setType(request.getType());
        customer.setRegion(request.getRegion()); // 保存原始地区名称
        customer.setIndustry(request.getIndustry());
        customer.setCompany(request.getCompany());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setCreditRating(request.getCreditRating());
        customer.setContactId(contactId);
        customer.setRemarks(request.getRemarks());
        customer.setCreatedAt(createdAt);
        customersMapper.insert(customer);

        // 8. 处理附件
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (String filename : request.getAttachments()) {
                attachments attachment = new attachments();
                attachment.setCustomerId(customerId);
                attachment.setFileName(filename);
                attachment.setFilePath("/uploads/customers/" + customerId + "/" + filename);
                attachmentsMapper.insert(attachment);
            }
        }
        // 新客户注册
        ActivityService activityService = SpringContextHolder.getBean(ActivityService.class);
        activityService.recordActivity(
                "新客户注册",                  // titleAct：活动标题
                "客户名称：" + customer.getName(),  // 描述：包含客户名称
                "客户管理",                    // 模块名（固定）
                "blue"                         // 颜色（固定）
        );
        return customerId;
    }

    //将地区名称转换为对应的地区代码
    private String getRegionCode(String regionName) {
        // 实际应用中可改为从数据库或配置文件读取
        Map<String, String> regionMap = new HashMap<>();
        regionMap.put("东北地区", "NE");
        regionMap.put("华北地区", "NC");
        regionMap.put("华东地区", "EC");
        regionMap.put("华南地区", "SC");
        regionMap.put("西南地区", "SW");
        regionMap.put("西北地区", "NW");
        regionMap.put("中南地区", "CN");
        regionMap.put("港澳台地区", "HMT");
        regionMap.put("其他区域", "OTH");
        return regionMap.get(regionName);
    }

    //获取指定地区的最大流水号
    private int getMaxSerialNumberByRegion(String regionCode) {
        QueryWrapper<customers> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("id", "CUS" + regionCode);
        queryWrapper.select("MAX(SUBSTRING(id, 6)) as maxSerial"); // 截取后5位流水号

        List<Map<String, Object>> result = customersMapper.selectMaps(queryWrapper);

        // 关键修改：处理列表为空或元素为null的情况
        if (result == null || result.isEmpty() || result.get(0) == null) {
            return 0; // 该地区无客户，从0开始
        }

        Object maxSerial = result.get(0).get("maxSerial");
        // 处理maxSerial本身为null的情况（如无数据时）
        return (maxSerial != null) ? Integer.parseInt(maxSerial.toString()) : 0;
    }


    //查询客户分页
    @Override
    public CustomerListResponse getCustomerList(int pageIndex, Integer pageSize,
                                                String name, String region, String industry, String creditRating) {
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
        if (region != null && !region.trim().isEmpty()&&!"all".equals(region.trim())) {
            queryWrapper.eq("region", region.trim());
        }

        // 按行业筛选
        if (industry != null && !industry.trim().isEmpty()&&!"all".equals(industry.trim())) {
            queryWrapper.eq("industry", industry.trim());
        }

        // 按信用筛选
        if (creditRating != null && !creditRating.trim().isEmpty()&&!"all".equals(creditRating.trim())) {
            queryWrapper.eq("creditRating", creditRating.trim());
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

    //查询客户详情
    @Override
    public CustomerDetailResponse getCustomerDetail(String customerId) {
        // 1. 查询客户基本信息
        customers customer = customersMapper.selectById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在: " + customerId);
        }

        // 2. 查询联系人信息
        List<contacts> contactList = new ArrayList<>();
        if (customer.getContactId() != 0) {
            contacts contact = contactsMapper.selectById(customer.getContactId());
            if (contact != null) {
                contactList.add(contact);
            }
        }

        // 3. 查询附件信息
        QueryWrapper<attachments> attachmentQuery = Wrappers.query();
        attachmentQuery.eq("customerId", customerId);
        List<attachments> attachmentList = attachmentsMapper.selectList(attachmentQuery);

        // 4. 转换为响应DTO
        CustomerDetailResponse response = new CustomerDetailResponse();
        CustomerDetailResponse.Info info = new CustomerDetailResponse.Info();

        // 设置客户基本信息
        info.setId(customer.getId());
        info.setName(customer.getName());
        info.setType(customer.getType());
        info.setRegion(customer.getRegion());
        info.setIndustry(customer.getIndustry());
        info.setCompany(customer.getCompany());
        info.setPhone(customer.getPhone());
        info.setAddress(customer.getAddress());
        info.setCreditRating(customer.getCreditRating());
        info.setCreatedAt(customer.getCreatedAt());
        info.setRemarks(customer.getRemarks());
        info.setModifiedAt(customer.getModifiedAt()); // 数据库是字符串，直接返回
        info.setModifiedBy(customer.getModifiedBy());

        // 设置联系人信息
        List<CustomerDetailResponse.ContactDTO> contactDTOs = contactList.stream().map(contact -> {
            CustomerDetailResponse.ContactDTO dto = new CustomerDetailResponse.ContactDTO();
            dto.setId(String.valueOf(contact.getId()));
            dto.setName(contact.getName());
            dto.setPosition(contact.getPosition());
            dto.setPhone(contact.getPhone());
            dto.setEmail(contact.getEmail());
            return dto;
        }).collect(Collectors.toList());
        info.setContacts(contactDTOs);

        // 设置附件信息
        List<CustomerDetailResponse.AttachmentDTO> attachmentDTOs = attachmentList.stream().map(attachment -> {
            CustomerDetailResponse.AttachmentDTO dto = new CustomerDetailResponse.AttachmentDTO();
            dto.setFilename(attachment.getFileName());
            dto.setFilepath(attachment.getFilePath());
            return dto;
        }).collect(Collectors.toList());
        info.setAttachments(attachmentDTOs);

        response.setInfo(info);
        return response;
    }

    //上传文件
    @Transactional
    @Override
    public FileUploadResponse uploadAttachments(String customerId, MultipartFile[] files) {
        // 1. 验证客户是否存在
        customers customer = customersMapper.selectById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在: " + customerId);
        }

        // 2. 创建客户专属文件夹（如：/uploads/C1023/）
        String customerUploadDir = baseUploadPath + File.separator + customerId;
        File dir = new File(customerUploadDir);
        if (!dir.exists()) {
            boolean mkdirs = dir.mkdirs(); // 递归创建目录
            if (!mkdirs) {
                throw new RuntimeException("创建上传目录失败: " + customerUploadDir);
            }
        }

        // 3. 处理上传文件
        List<FileUploadResponse.AttachmentDTO> attachmentList = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue; // 跳过空文件
            }

            try {
                // 3.1 获取原始文件名
                String originalFilename = file.getOriginalFilename();

                // 3.2 处理文件名（可选项：添加UUID避免重名）
                String filename = originalFilename;
                // 如需避免重名可启用下面代码
                // String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                // String filename = UUID.randomUUID().toString() + extension;

                // 3.3 保存文件到本地
                String filePath = customerUploadDir + File.separator + filename;
                File destFile = new File(filePath);
                file.transferTo(destFile);

                // 3.4 记录到附件表
                attachments attachment = new attachments();
                attachment.setCustomerId(customerId);
                attachment.setFileName(originalFilename); // 保存原始文件名
                attachment.setFilePath(customerId + "/" + filename); // 存储相对路径
                attachmentsMapper.insert(attachment);

                // 3.5 添加到响应列表
                FileUploadResponse.AttachmentDTO dto = new FileUploadResponse.AttachmentDTO();
                dto.setFilename(originalFilename);
                dto.setFilepath(attachment.getFilePath());
                attachmentList.add(dto);

            } catch (IOException e) {
                throw new RuntimeException("文件上传失败: " + e.getMessage());
            }
        }

        // 4. 构建并返回响应
        FileUploadResponse response = new FileUploadResponse();
        FileUploadResponse.FileUploadData data = new FileUploadResponse.FileUploadData();
        data.setAttachments(attachmentList);
        response.setData(data);

        return response;
    }


}

