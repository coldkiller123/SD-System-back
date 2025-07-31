package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.example.erp.dto.InquiryCreateDTO;
import org.example.erp.dto.InquiryPageResult;
import org.example.erp.dto.InquiryQueryParam;
import org.example.erp.dto.InquiryStatusUpdateDTO;
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

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InquiryServiceImpl extends ServiceImpl<inquiriesMapper, inquiries> implements InquiryService {

    private static final Log log = LogFactory.getLog(InquiryServiceImpl.class);

    // 跟踪当前年份和流水号（原子类保证线程安全）
    private int currentYear;
    private AtomicInteger serialNumber;

    @Autowired
    private customersMapper customersMapper;

    @Autowired
    private productsMapper productsMapper;

    /**
     * 从数据库查询指定年份的最大流水号（精确匹配格式）
     */
    private int queryMaxSerialFromDB(int year) {
        QueryWrapper<inquiries> queryWrapper = new QueryWrapper<>();
        // 严格匹配"RFQ+年份+5位数字"格式，避免错误匹配
        String pattern = "RFQ" + year + "\\d{5}";
        queryWrapper.apply("inquiryId REGEXP {0}", pattern);
        queryWrapper.select("MAX(CAST(SUBSTRING(inquiryId, 6) AS UNSIGNED)) as maxSerial");

        List<Map<String, Object>> result = baseMapper.selectMaps(queryWrapper);

        if (result == null || result.isEmpty() || result.get(0) == null) {
            return 0;
        }

        Object maxSerialObj = result.get(0).get("maxSerial");
        if (maxSerialObj == null) {
            return 0;
        }

        try {
            int maxSerial = Integer.parseInt(maxSerialObj.toString());
            return maxSerial > 99999 ? 1 : maxSerial; // 限制5位
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 初始化：从数据库最大ID+1开始，确保初始值不重复
     */
    @PostConstruct
    public void init() {
        Calendar calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);
        // 初始流水号 = 数据库最大流水号 + 1（彻底避免重复）
        int maxSerial = queryMaxSerialFromDB(currentYear);
        serialNumber = new AtomicInteger(maxSerial > 0 ? maxSerial + 1 : 1);
    }

    /**
     * 创建询价单：生成ID前先检查数据库，确保唯一
     */
    @Override
    @Transactional
    public inquiries createInquiry(InquiryCreateDTO inquiryDTO) {
        // 1. 验证客户和商品存在性
        customers customer = customersMapper.selectById(inquiryDTO.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在：" + inquiryDTO.getCustomerId());
        }
        products product = productsMapper.selectById(inquiryDTO.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在：" + inquiryDTO.getProductId());
        }

        // 2. 生成唯一ID（核心：先检查再使用）
        String inquiryId = generateUniqueInquiryId();

        // 3. 构建实体并保存
        inquiries inquiry = new inquiries();
        BeanUtils.copyProperties(inquiryDTO, inquiry);
        inquiry.setInquiryId(inquiryId);
        inquiry.setStatus("未报价");
        inquiry.setCustomerName(customer.getName());

        baseMapper.insert(inquiry);
        return inquiry;
    }

    /**
     * 生成唯一ID的核心方法：先计算候选ID，再检查数据库是否存在，不存在则使用
     */
    private synchronized String generateUniqueInquiryId() {
        while (true) { // 循环直到生成不存在的ID
            Calendar calendar = Calendar.getInstance();
            int currentYearNow = calendar.get(Calendar.YEAR);

            // 跨年份或流水号超限（≥99999）时重置
            if (currentYearNow != currentYear || serialNumber.get() > 99999) {
                currentYear = currentYearNow;
                int maxSerial = queryMaxSerialFromDB(currentYear);
                serialNumber.set(maxSerial > 0 ? maxSerial + 1 : 1);
            }

            // 计算候选流水号和ID
            int currentSerial = serialNumber.get();
            String candidateId = String.format("RFQ%d%05d", currentYear, currentSerial);

            // 检查数据库中是否已存在该ID
            LambdaQueryWrapper<inquiries> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(inquiries::getInquiryId, candidateId);
            long count = baseMapper.selectCount(queryWrapper);

            if (count == 0) {
                // ID不存在，使用当前流水号并递增
                serialNumber.incrementAndGet();
                return candidateId;
            } else {
                // ID已存在，日志记录并跳过该流水号
                log.warn("候选ID已存在，自动跳过：" + candidateId);
                serialNumber.incrementAndGet(); // 强制递增，避免死循环
            }
        }
    }

    // 分页查询和状态更新方法保持不变
    @Override
    public InquiryPageResult getInquiries(InquiryQueryParam queryParam) {
        Page<inquiries> page = new Page<>(queryParam.getPageIndex() + 1, queryParam.getPageSize());
        LambdaQueryWrapper<inquiries> queryWrapper = new LambdaQueryWrapper<>();

        if (queryParam.getInquiryId() != null && !queryParam.getInquiryId().isEmpty()) {
            queryWrapper.like(inquiries::getInquiryId, queryParam.getInquiryId());
        }
        if (queryParam.getCustomerName() != null && !queryParam.getCustomerName().isEmpty()) {
            queryWrapper.like(inquiries::getCustomerName, queryParam.getCustomerName());
        }
        if (queryParam.getStatus() != null && !queryParam.getStatus().isEmpty()) {
            queryWrapper.eq(inquiries::getStatus, queryParam.getStatus());
        }

        Page<inquiries> resultPage = baseMapper.selectPage(page, queryWrapper);

        InquiryPageResult pageResult = new InquiryPageResult();
        pageResult.setInquiries(resultPage.getRecords());
        pageResult.setTotal((int) resultPage.getTotal());
        pageResult.setPageCount((int) Math.ceil((double) resultPage.getTotal() / queryParam.getPageSize()));

        return pageResult;
    }

    @Override
    @Transactional
    public inquiries updateInquiryStatus(String inquiryId, InquiryStatusUpdateDTO updateDTO) {
        inquiries inquiry = baseMapper.selectById(inquiryId);
        if (inquiry == null) {
            throw new IllegalArgumentException("询价单不存在：" + inquiryId);
        }

        if (!"未报价".equals(inquiry.getStatus())) {
            throw new IllegalArgumentException("仅“未报价”状态可更新为“已报价”，当前状态：" + inquiry.getStatus());
        }
        if (!"已报价".equals(updateDTO.getStatus())) {
            throw new IllegalArgumentException("状态更新不合法，仅支持更新为“已报价”");
        }

        inquiry.setStatus(updateDTO.getStatus());
        baseMapper.updateById(inquiry);

        return inquiry;
    }
}