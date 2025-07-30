package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.erp.dto.InvoiceGenerateResponse;
import org.example.erp.entity.customers;
import org.example.erp.entity.invoices;
import org.example.erp.entity.orders;
import org.example.erp.entity.products;
import org.example.erp.mapper.customersMapper;
import org.example.erp.mapper.invoicesMapper;
import org.example.erp.mapper.ordersMapper;
import org.example.erp.mapper.productsMapper;
import org.example.erp.service.ActivityService;
import org.example.erp.service.InvoiceService;
import org.example.erp.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private ordersMapper ordersMapper;

    @Autowired
    private customersMapper customersMapper;

    @Autowired
    private productsMapper productsMapper;

    @Autowired
    private invoicesMapper invoicesMapper;

    // 当天日期（用于判断是否跨天）
    private String currentDate;
    // 当天流水号（原子类保证线程安全）
    private AtomicInteger dailySerial;
    // 锁：解决并发和跨天重置问题
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 初始化：服务启动时加载当天天已有最大流水号
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        String today = getCurrentDateStr();
        int maxSerial = queryMaxDailySerial(today);
        currentDate = today;
        dailySerial = new AtomicInteger(maxSerial > 0 ? maxSerial : 1);
    }

    /**
     * 获取当前日期字符串（YYYYMMDD）
     */
    private String getCurrentDateStr() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    /**
     * 查询指定日期的最大流水号
     */
    private int queryMaxDailySerial(String dateStr) {
        QueryWrapper<invoices> queryWrapper = new QueryWrapper<>();
        // 发票编号格式：INV20250728-051，前缀为"INV+日期+"
        queryWrapper.likeRight("invoiceId", "INV" + dateStr + "-");
        // 截取流水号部分（从第12位开始："INV20250728-"是11位）
        queryWrapper.select("MAX(SUBSTRING(invoiceId, 12)) as maxSerial");

        List<Map<String, Object>> result = invoicesMapper.selectMaps(queryWrapper);
        if (result == null || result.isEmpty() || result.get(0) == null) {
            return 0;
        }

        Object maxSerialObj = result.get(0).get("maxSerial");
        if (maxSerialObj == null) {
            return 0;
        }

        try {
            return Integer.parseInt(maxSerialObj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 生成发票编号：INV + 日期（YYYYMMDD） + "-" + 三位流水号
     */
    private String generateInvoiceId() {
        String today = getCurrentDateStr();
        lock.lock();
        try {
            // 跨天时重置流水号
            if (!today.equals(currentDate)) {
                int maxSerial = queryMaxDailySerial(today);
                currentDate = today;
                dailySerial.set(maxSerial > 0 ? maxSerial : 1);
            }
            // 生成当天流水号（自增）
            int serial = dailySerial.getAndIncrement();
            // 格式化为3位（不足补0）
            return String.format("INV%s-%03d", today, serial);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    @Override
    public InvoiceGenerateResponse generateInvoice(String orderId) {
        // 1. 查询订单信息
        orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在: " + orderId);
        }
        // 校验订单状态是否为“已完成”
        if (!"已完成".equals(order.getStatus())) {
            throw new RuntimeException("订单" + orderId + "状态不是“已完成”，无法生成发票");
        }

        // 2. 查询客户信息
        customers customer = customersMapper.selectById(order.getCustomerId());
        if (customer == null) {
            throw new RuntimeException("客户不存在: " + order.getCustomerId());
        }

        // 3. 查询产品信息
        products product = productsMapper.selectById(order.getProductId());
        if (product == null) {
            throw new RuntimeException("产品不存在: " + order.getProductId());
        }

        // 4. 生成发票ID（使用新规则生成）
        String invoiceId = generateInvoiceId();

        // 5. 计算日期（开具日期为当前时间，截止日期为30天后）
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime dueDate = now.plusDays(30);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        // 6. 保存发票记录到数据库
        invoices invoice = new invoices();
        invoice.setInvoiceId(invoiceId);
        invoice.setOrderId(orderId);
        invoice.setIssueDate(now.format(formatter));
        invoice.setDueDate(dueDate.format(formatter));
        invoice.setTaxRate(new BigDecimal("0.13"));
        invoice.setTotalAmount(order.getTotalAmount());
        invoice.setPaidAmount(order.getPaidAmount());
        invoice.setDueAmount(order.getTotalAmount().subtract(order.getPaidAmount()));
        invoicesMapper.insert(invoice);
        order.setHasInvoice(true); // 设置为已开票
        ordersMapper.updateById(order); // 保存更新

        // 7. 构建响应DTO
        InvoiceGenerateResponse response = new InvoiceGenerateResponse();
        response.setInvoiceId(invoiceId);
        response.setOrderId(orderId);
        response.setIssueDate(now.format(formatter));
        response.setDueDate(dueDate.format(formatter));
        response.setTaxRate(new BigDecimal("0.13"));
        response.setStatus("已完成");

        // 8. 设置客户信息
        InvoiceGenerateResponse.Customer customerDTO = new InvoiceGenerateResponse.Customer();
        customerDTO.setId(customer.getId());
        customerDTO.setName(customer.getName());
        customerDTO.setAddress(customer.getAddress());
        // 假设客户表中有taxId字段，如果没有可以设置为默认值
        customerDTO.setTaxId(customer.getTaxId() != null ? customer.getTaxId() : "TAX" + System.currentTimeMillis());
        response.setCustomer(customerDTO);

        // 9. 设置商品信息
        List<InvoiceGenerateResponse.Item> items = new ArrayList<>();
        InvoiceGenerateResponse.Item item = new InvoiceGenerateResponse.Item();
        item.setId(product.getId());
        item.setName(product.getName());
        item.setQuantity(order.getQuantity());
        item.setUnitPrice(product.getUnitPrice());
        item.setDescription(product.getDescription());
        items.add(item);
        response.setItems(items);
        // 记录活动日志
        ActivityService activityService = SpringContextHolder.getBean(ActivityService.class);
        activityService.recordActivity(
                "新发票开具",
                "发票号：" + invoiceId,
                "财务管理",
                "purple"
        );

        return response;
    }
}
