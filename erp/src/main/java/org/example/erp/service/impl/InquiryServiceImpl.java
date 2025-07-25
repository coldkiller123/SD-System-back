package org.example.erp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.erp.dto.InquiryCreateDTO;
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
}
