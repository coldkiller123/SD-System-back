package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InquiryServiceImpl extends ServiceImpl<inquiriesMapper, inquiries> implements InquiryService {

    // 新增：跟踪当前年份和流水号（原子类保证线程安全）
    private int currentYear;
    private AtomicInteger serialNumber;
    @Autowired
    private customersMapper customersMapper;

    @Autowired
    private productsMapper productsMapper;
    /**
     * 从数据库查询指定年份的最大流水号
     * @param year 年份（如2025）
     * @return 最大流水号（无记录返回0）
     */
    private int queryMaxSerialFromDB(int year) {
        // 构建查询条件：询价单ID以"RFQ+年份"开头（如"RFQ2025"）
        QueryWrapper<inquiries> queryWrapper = new QueryWrapper<>();
        // 条件：询价单ID以"RFQ+年份"开头（直接传入数据库字段名）
        queryWrapper.likeRight("inquiryId", "RFQ" + year);
        // 指定查询字段（SQL片段）
        queryWrapper.select("MAX(SUBSTRING(inquiryId, 6)) as maxSerial");

        // 执行查询
        List<Map<String, Object>> result = baseMapper.selectMaps(queryWrapper);

        // 处理查询结果（避免空指针）
        if (result == null || result.isEmpty() || result.get(0) == null) {
            return 0;
        }

        Object maxSerialObj = result.get(0).get("maxSerial");
        if (maxSerialObj == null) {
            return 0;
        }

        // 转换为整数（流水号是数字字符串，如"00009"）
        try {
            return Integer.parseInt(maxSerialObj.toString());
        } catch (NumberFormatException e) {
            return 0; // 格式错误时默认返回0
        }
    }
    // 新增：初始化方法，服务启动时加载当年最大流水号
    @PostConstruct // 自动执行初始化
    public void init() {
        Calendar calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR); // 获取当前年份（如2025）

        // 从数据库查询当年最大流水号
        int maxSerial = queryMaxSerialFromDB(currentYear);

        // 初始化流水号（无记录则从1开始）
        serialNumber = new AtomicInteger(maxSerial > 0 ? maxSerial : 1);
    }

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

        // 3. 生成询价单ID（新规则：RFQ + 年份 + 5位流水号）
        Calendar calendar = Calendar.getInstance();
        int currentYearNow = calendar.get(Calendar.YEAR);

        // 跨年份时重置流水号
        if (currentYearNow != currentYear) {
            synchronized (this) { // 加锁保证线程安全
                if (currentYearNow != currentYear) {
                    currentYear = currentYearNow;
                    // 重新查询新年份的最大流水号
                    int maxSerial = queryMaxSerialFromDB(currentYear);
                    serialNumber.set(maxSerial > 0 ? maxSerial : 1);
                }
            }
        }

// 获取当前流水号并递增
        int currentSerial = serialNumber.getAndIncrement();
// 格式化生成ID（RFQ + 年份 + 5位流水号，不足补0）
        String inquiryId = String.format("RFQ%d%05d", currentYear, currentSerial);

        // 4. 复制DTO数据到实体类
        inquiries inquiry = new inquiries();
        BeanUtils.copyProperties(inquiryDTO, inquiry);
        inquiry.setInquiryId(inquiryId);
        inquiry.setStatus("未报价"); // 默认状态
        inquiry.setCustomerName(customer.getName());

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
    /**
     * 更新询价单状态（从“未报价”到“已报价”）
     */
    @Override
    @Transactional
    public inquiries updateInquiryStatus(String inquiryId, InquiryStatusUpdateDTO updateDTO) {
        // 1. 验证询价单是否存在
        inquiries inquiry = baseMapper.selectById(inquiryId);
        if (inquiry == null) {
            throw new IllegalArgumentException("询价单不存在：" + inquiryId);
        }

        // 2. 验证状态变更合法性（仅允许从“未报价”更新为“已报价”）
        if (!"未报价".equals(inquiry.getStatus())) {
            throw new IllegalArgumentException("仅“未报价”状态可更新为“已报价”，当前状态：" + inquiry.getStatus());
        }
        if (!"已报价".equals(updateDTO.getStatus())) {
            throw new IllegalArgumentException("状态更新不合法，仅支持更新为“已报价”");
        }

        // 3. 更新状态
        inquiry.setStatus(updateDTO.getStatus());
        baseMapper.updateById(inquiry);

        return inquiry;
    }
}
