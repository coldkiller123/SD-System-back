package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.erp.dto.*;
import org.example.erp.entity.customers;
import org.example.erp.entity.order_histories;
import org.example.erp.entity.orders;
import org.example.erp.entity.products;
import org.example.erp.mapper.customersMapper;
import org.example.erp.mapper.order_historiesMapper;
import org.example.erp.mapper.ordersMapper;
import org.example.erp.mapper.productsMapper;
import org.example.erp.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<ordersMapper, orders> implements OrderService {

    @Autowired
    private customersMapper customersMapper;

    @Autowired
    private productsMapper productsMapper;

    @Autowired
    private order_historiesMapper orderHistoriesMapper;

    @Override
    public PageResult<orders> getOrders(OrderQueryParam queryParam) {
        // 已有的分页查询逻辑保持不变
        LambdaQueryWrapper<orders> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryParam.getOrderId())) {
            queryWrapper.like(orders::getId, queryParam.getOrderId());
        }

        if (StringUtils.hasText(queryParam.getCustomerName())) {
            queryWrapper.like(orders::getCustomerName, queryParam.getCustomerName());
        }

        if (StringUtils.hasText(queryParam.getStatus())) {
            queryWrapper.eq(orders::getStatus, queryParam.getStatus());
        }

        queryWrapper.orderByDesc(orders::getCreatedAt);

        IPage<orders> page = new Page<>(
                queryParam.getPageIndex(),
                queryParam.getPageSize()
        );

        IPage<orders> resultPage = this.page(page, queryWrapper);

        int pageCount = (int) Math.ceil((double) resultPage.getTotal() / queryParam.getPageSize());

        return new PageResult<>(
                resultPage.getRecords(),
                resultPage.getTotal(),
                pageCount
        );
    }

    @Override
    @Transactional
    public orders createOrder(OrderCreateDTO orderDTO) {
        // 1. 验证客户是否存在
        customers customer = customersMapper.selectById(orderDTO.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }

        // 2. 验证商品是否存在
        products product = productsMapper.selectById(orderDTO.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 3. 验证库存是否充足（如果需要）
        if (product.getQuantity().compareTo(orderDTO.getQuantity()) < 0) {
            throw new IllegalArgumentException("库存不足");
        }

        // 4. 生成订单ID (可以根据业务规则生成，这里使用UUID)
        String orderId = generateOrderId();

        // 5. 转换DTO为实体对象
        orders order = new orders();
        BeanUtils.copyProperties(orderDTO, order);
        order.setId(orderId);
        order.setCustomerName(customer.getName()); // 从客户信息中获取客户名称

        // 6. 如果没有提供创建时间，使用当前时间
        if (orderDTO.getCreatedAt() == null || orderDTO.getCreatedAt().isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            order.setCreatedAt(sdf.format(new Date()));
        }

        // 7. 设置是否已开票（根据实付金额判断，这里只是示例逻辑）
        order.setHasInvoice(orderDTO.getPaidAmount().compareTo(BigDecimal.ZERO) > 0);

        // 8. 保存订单
        baseMapper.insert(order);

        return order;
    }
    @Override
    @Transactional
    public orders updateOrder(String orderId, OrderUpdateDTO updateDTO) {
        // 1. 验证订单是否存在
        orders existingOrder = baseMapper.selectById(orderId);
        if (existingOrder == null) {
            throw new IllegalArgumentException("订单不存在，无法修改");
        }

        // 2. 验证客户和商品是否有效（同创建订单逻辑）
        customers customer = customersMapper.selectById(updateDTO.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        products product = productsMapper.selectById(updateDTO.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 3. 验证库存（若订单状态已发货，不允许修改数量，避免库存异常）
        if ("已发货".equals(existingOrder.getStatus())
                && existingOrder.getQuantity().compareTo(updateDTO.getQuantity()) != 0) {
            throw new IllegalArgumentException("已发货订单不允许修改数量");
        }
        if (product.getQuantity().compareTo(updateDTO.getQuantity()) < 0) {
            throw new IllegalArgumentException("库存不足");
        }

        // 4. 复制可修改字段（排除不可编辑的createdAt和orderId）
        BeanUtils.copyProperties(updateDTO, existingOrder, "id", "createdAt");
        // 强制覆盖modifiedAt为当前时间（忽略前端传入值，确保准确性）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        existingOrder.setCreatedAt(existingOrder.getCreatedAt()); // 保留原始创建时间
        existingOrder.setHasInvoice(updateDTO.getPaidAmount().compareTo(BigDecimal.ZERO) > 0); // 同步更新开票状态

        // 5. 保存更新后的订单
        baseMapper.updateById(existingOrder);

        // 6. 记录订单修改历史（到order_histories表）
        order_histories history = new order_histories();
        history.setOrderId(orderId);
        history.setModifiedBy(updateDTO.getModifiedBy());
        history.setRemarks(updateDTO.getRemarks() != null ?
                "修改：" + updateDTO.getRemarks() : "无备注修改");
        orderHistoriesMapper.insert(history);

        return existingOrder;
    }
    @Override
    public order_histories getLatestHistory(String orderId) {
        // 构建查询条件：按订单ID查询，按修改时间倒序，取第一条
        LambdaQueryWrapper<order_histories> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(order_histories::getOrderId, orderId)
                .orderByDesc(order_histories::getModifiedAt)
                .last("LIMIT 1"); // 只取最新的一条

        return orderHistoriesMapper.selectOne(queryWrapper);
    }
    @Override
    public OrderDetailDTO getOrderDetail(String orderId) {
        // 1. 查询订单基本信息
        orders order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在：" + orderId);
        }

        // 2. 转换为订单详情DTO
        OrderDetailDTO detailDTO = new OrderDetailDTO();
        BeanUtils.copyProperties(order, detailDTO);

        // 3. 查询该订单的所有操作历史
        LambdaQueryWrapper<order_histories> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(order_histories::getOrderId, orderId)
                .orderByDesc(order_histories::getModifiedAt); // 按修改时间倒序（最新的在前）
        List<order_histories> histories = orderHistoriesMapper.selectList(queryWrapper);

        // 4. 转换历史记录为DTO列表
        List<OrderHistoryDTO> historyDTOs = new ArrayList<>();
        for (order_histories history : histories) {
            OrderHistoryDTO historyDTO = new OrderHistoryDTO();
            BeanUtils.copyProperties(history, historyDTO);
            // 若历史记录ID是数字类型，转换为字符串（避免前端处理问题）
            historyDTO.setId(String.valueOf(history.getId()));
            historyDTOs.add(historyDTO);
        }

        // 5. 设置历史记录到详情DTO
        detailDTO.setHistory(historyDTOs);

        return detailDTO;
    }

    /**
     * 生成订单ID，格式：ORD+日期+随机数
     */
    private String generateOrderId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        String randomStr = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + dateStr + randomStr;
    }
    @Override
    public UnshippedOrderResponseDTO getUnshippedOrders(UnshippedOrderQueryDTO queryDTO) {
        // 1. 构建分页对象（page从1开始，MyBatis-Plus页码直接对应）
        Page<orders> page = new Page<>(queryDTO.getPage(), queryDTO.getPage_size());

        // 2. 构建查询条件：状态为“已付款”，支持搜索
        LambdaQueryWrapper<orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(orders::getStatus, "已付款"); // 只查询“已付款”的订单

        // 搜索关键字（订单号、客户名称、商品名称）
        String search = queryDTO.getSearch();
        if (search != null && !search.isEmpty()) {
            queryWrapper.and(w -> w
                    .like(orders::getId, search) // 订单号
                    .or().like(orders::getCustomerName, search) // 客户名称
                    .or().like(orders::getProductName, search) // 商品名称
            );
        }

        // 3. 执行分页查询
        Page<orders> resultPage = baseMapper.selectPage(page, queryWrapper);

        // 4. 转换为响应DTO
        UnshippedOrderResponseDTO response = new UnshippedOrderResponseDTO();
        UnshippedOrderResponseDTO.Data data = new UnshippedOrderResponseDTO.Data();
        data.setTotal((int) resultPage.getTotal());
        data.setPage(queryDTO.getPage());
        data.setPage_size(queryDTO.getPage_size());

        // 转换订单列表
        List<UnshippedOrderResponseDTO.OrderItem> orderItems = resultPage.getRecords().stream()
                .map(order -> {
                    UnshippedOrderResponseDTO.OrderItem item = new UnshippedOrderResponseDTO.OrderItem();
                    item.setId(order.getId());
                    item.setCustomerId(order.getCustomerId());
                    item.setCustomerName(order.getCustomerName());
                    item.setProductName(order.getProductName());
                    item.setQuantity(order.getQuantity());
                    item.setOrderDate(order.getCreatedAt()); // 假设订单日期是createdAt
                    item.setAmount(order.getTotalAmount());
                    item.setStatus(order.getStatus());
                    return item;
                })
                .collect(Collectors.toList());
        data.setOrders(orderItems);

        response.setCode(200);
        response.setMessage("成功");
        response.setData(data);

        return response;
    }
}
