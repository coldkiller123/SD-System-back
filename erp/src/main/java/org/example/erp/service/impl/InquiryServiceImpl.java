package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.erp.dto.InquiryCreateDTO;
import org.example.erp.dto.InquiryPageResult;
import org.example.erp.dto.InquiryQueryParam;
import org.example.erp.entity.customers;
import org.example.erp.entity.inquiries;
import org.example.erp.entity.products;
import org.example.erp.mapper.customersMapper;
import org.example.erp.mapper.inquiriesMapper;
import org.example.erp.mapper.productsMapper;
import org.example.erp.service.InquiryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class InquiryServiceImpl extends ServiceImpl<inquiriesMapper, inquiries> implements InquiryService {

    @Autowired
    private customersMapper customersMapper;

    @Autowired
    private productsMapper productsMapper;

    @Override
    @Transactional
    public inquiries createInquiry(InquiryCreateDTO inquiryDTO) {
        // 1. 验证客户是否存在
        customers customer = customersMapper.selectById(inquiryDTO.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在：" + inquiryDTO.getCustomerId());
        }

        // 2. 验证商品是否存在
        products product = productsMapper.selectById(inquiryDTO.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在：" + inquiryDTO.getProductId());
        }

        // 3. 生成询价单ID（格式：IQ + 日期 + 随机数）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        String randomStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6);
        String inquiryId = "IQ" + dateStr + randomStr;

        // 4. 复制DTO数据到实体类
        inquiries inquiry = new inquiries();
        BeanUtils.copyProperties(inquiryDTO, inquiry);
        inquiry.setInquiryId(inquiryId);
        inquiry.setStatus("未报价"); // 默认状态

        // 5. 保存到数据库
        baseMapper.insert(inquiry);

        return inquiry;
    }
    @Override
    public InquiryPageResult getInquiries(InquiryQueryParam queryParam) {
        // 1. 构建分页对象（pageIndex从0开始，MyBatis-Plus页码从1开始，需+1）
        Page<inquiries> page = new Page<>(queryParam.getPageIndex() + 1, queryParam.getPageSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<inquiries> queryWrapper = new LambdaQueryWrapper<>();
        // 询价单号模糊搜索
        if (queryParam.getInquiryId() != null && !queryParam.getInquiryId().isEmpty()) {
            queryWrapper.like(inquiries::getInquiryId, queryParam.getInquiryId());
        }
        // 客户名称模糊搜索
        if (queryParam.getCustomerName() != null && !queryParam.getCustomerName().isEmpty()) {
            queryWrapper.like(inquiries::getCustomerName, queryParam.getCustomerName());
        }
        // 状态筛选
        if (queryParam.getStatus() != null && !queryParam.getStatus().isEmpty()) {
            queryWrapper.eq(inquiries::getStatus, queryParam.getStatus());
        }

        // 3. 执行分页查询
        Page<inquiries> resultPage = baseMapper.selectPage(page, queryWrapper);

        // 4. 组装分页结果
        InquiryPageResult pageResult = new InquiryPageResult();
        pageResult.setInquiries(resultPage.getRecords()); // 当前页数据
        pageResult.setTotal((int) resultPage.getTotal()); // 总记录数
        // 计算总页数（总记录数÷每页条数，向上取整）
        pageResult.setPageCount((int) Math.ceil((double) resultPage.getTotal() / queryParam.getPageSize()));

        return pageResult;
    }
}
