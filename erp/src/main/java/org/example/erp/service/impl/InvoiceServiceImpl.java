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
            if (!today.equals(currentDate)) {
                // 跨天查询时，先检查数据库最新序号（避免遗漏）
                int maxSerial = queryMaxDailySerial(today);
                currentDate = today;
                dailySerial.set(maxSerial > 0 ? maxSerial : 1);
            }

            // 生成序号后，再次校验数据库是否已存在（双重保险）
            int serial;
            String invoiceId;
            do {
                serial = dailySerial.getAndIncrement();
                invoiceId = String.format("INV%s-%03d", today, serial);
            } while (checkInvoiceIdExists(invoiceId)); // 检查是否已存在

            return invoiceId;
        } finally {
            lock.unlock();
        }
    }

    // 检查发票ID是否已存在
    private boolean checkInvoiceIdExists(String invoiceId) {
        return invoicesMapper.selectById(invoiceId) != null;
    }

    @Transactional
    @Override
    public InvoiceGenerateResponse generateInvoice(String orderId) {
        // 1. 查询订单信息
        orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在: " + orderId);
        }

        // 2. 检查是否已开具发票
        if (order.isHasInvoice()) {
            // 已开票：查询已有发票（改用QueryWrapper手动构建查询，避免方法名解析问题）
            QueryWrapper<invoices> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("orderId", orderId); // 明确指定查询条件：order_id = 订单ID
            invoices existingInvoice = invoicesMapper.selectOne(queryWrapper); // 使用selectOne查询唯一结果

            if (existingInvoice == null) {
                throw new RuntimeException("订单" + orderId + "标记已开票，但未查询到发票记录");
            }
            // 构建已有发票的响应
            return buildInvoiceResponse(existingInvoice, order, orderId);
        }

        // 3. 未开票：校验订单状态
        if (!"已完成".equals(order.getStatus())) {
            throw new RuntimeException("订单" + orderId + "状态不是“已完成”，无法生成发票");
        }

        // 4. 查询客户信息
        customers customer = customersMapper.selectById(order.getCustomerId());
        if (customer == null) {
            throw new RuntimeException("客户不存在: " + order.getCustomerId());
        }

        // 5. 查询产品信息
        products product = productsMapper.selectById(order.getProductId());
        if (product == null) {
            throw new RuntimeException("产品不存在: " + order.getProductId());
        }

        // 6. 生成发票ID（使用原规则）
        String invoiceId = generateInvoiceId();

        // 7. 计算日期
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime dueDate = now.plusDays(30);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        // 8. 保存发票记录
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

        // 更新订单开票状态
        order.setHasInvoice(true);
        ordersMapper.updateById(order);

        // 9. 记录活动日志
        ActivityService activityService = SpringContextHolder.getBean(ActivityService.class);
        activityService.recordActivity(
                "新发票开具",
                "发票号：" + invoiceId,
                "财务管理",
                "purple"
        );

        // 10. 构建并返回新发票响应
        return buildInvoiceResponse(invoice, order, orderId);
    }
    // 新增：复用的响应构建方法（同时支持新发票和已有发票）
    private InvoiceGenerateResponse buildInvoiceResponse(invoices invoice, orders order, String orderId) {
        // 构建基础响应
        InvoiceGenerateResponse response = new InvoiceGenerateResponse();
        response.setInvoiceId(invoice.getInvoiceId());
        response.setOrderId(orderId);
        response.setIssueDate(invoice.getIssueDate());
        response.setDueDate(invoice.getDueDate());
        response.setTaxRate(invoice.getTaxRate());
        response.setStatus("已完成"); // 无论是新生成还是已存在，状态均为已完成

        // 设置客户信息
        customers customer = customersMapper.selectById(order.getCustomerId());
        InvoiceGenerateResponse.Customer customerDTO = new InvoiceGenerateResponse.Customer();
        customerDTO.setId(customer.getId());
        customerDTO.setName(customer.getName());
        customerDTO.setAddress(customer.getAddress());
        customerDTO.setTaxId(customer.getTaxId() != null ? customer.getTaxId() : "TAX" + System.currentTimeMillis());
        response.setCustomer(customerDTO);

        // 设置商品信息
        products product = productsMapper.selectById(order.getProductId());
        List<InvoiceGenerateResponse.Item> items = new ArrayList<>();
        InvoiceGenerateResponse.Item item = new InvoiceGenerateResponse.Item();
        item.setId(product.getId());
        item.setName(product.getName());
        item.setQuantity(order.getQuantity());
        item.setUnitPrice(product.getUnitPrice());
        item.setDescription(product.getDescription());
        items.add(item);
        response.setItems(items);

        return response;
    }
}
