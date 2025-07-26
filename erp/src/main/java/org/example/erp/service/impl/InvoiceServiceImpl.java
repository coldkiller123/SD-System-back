package org.example.erp.service.impl;

import org.example.erp.dto.InvoiceGenerateResponse;
import org.example.erp.entity.customers;
import org.example.erp.entity.invoices;
import org.example.erp.entity.orders;
import org.example.erp.entity.products;
import org.example.erp.mapper.customersMapper;
import org.example.erp.mapper.invoicesMapper;
import org.example.erp.mapper.ordersMapper;
import org.example.erp.mapper.productsMapper;
import org.example.erp.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Transactional
    @Override
    public InvoiceGenerateResponse generateInvoice(String orderId) {
        // 1. 查询订单信息
        orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在: " + orderId);
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

        // 4. 生成发票ID
        String invoiceId = "INV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

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

        // 7. 构建响应DTO
        InvoiceGenerateResponse response = new InvoiceGenerateResponse();
        response.setInvoiceId(invoiceId);
        response.setOrderId(orderId);
        response.setIssueDate(now.format(formatter));
        response.setDueDate(dueDate.format(formatter));
        response.setTaxRate(new BigDecimal("0.13"));
        response.setStatus("待付款");

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

        return response;
    }
}

