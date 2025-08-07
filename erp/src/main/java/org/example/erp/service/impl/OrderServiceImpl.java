package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import jakarta.annotation.PostConstruct;
import org.example.erp.dto.*;
import org.example.erp.entity.*;
import org.example.erp.mapper.*;
import org.example.erp.service.ActivityService;
import org.example.erp.service.OrderService;
import org.example.erp.dto.DeliveredOrdersResponse;
import org.example.erp.utils.SpringContextHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


@Service
public class OrderServiceImpl extends ServiceImpl<ordersMapper, orders> implements OrderService {

    private int currentYear;
    private AtomicInteger serialNumber;

    @Autowired
    private customersMapper customersMapper;

    @Autowired
    private productsMapper productsMapper;

    @Autowired
    private order_historiesMapper orderHistoriesMapper;

    @Autowired
    private ordersMapper ordersMapper;

    @Autowired
    private delivery_ordersMapper deliveryOrdersMapper;

    @Autowired
    private invoicesMapper invoicesMapper;

    @Autowired
    private delivery_order_itemsMapper deliveryOrderItemsMapper;
    // 订单ID生成相关变量（与发货单逻辑对齐）
    private String currentYearStr; // 当前年份字符串（YYYY）
    private AtomicInteger yearlySerial; // 年度流水号（原子类保证线程安全）
    private final ReentrantLock orderLock = new ReentrantLock(); // 锁对象

    /**
     * 初始化：服务启动时加载当前年份最大流水号
     */
    @PostConstruct
    public void initOrderIdGenerator() {
        String currentYear = getCurrentYearStr();
        int maxSerial = queryMaxYearlySerial(currentYear);
        currentYearStr = currentYear;
        yearlySerial = new AtomicInteger(maxSerial > 0 ? maxSerial : 1);
    }

    /**
     * 获取当前年份字符串（YYYY）
     */
    private String getCurrentYearStr() {
        return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    }

    /**
     * 查询指定年份的最大流水号
     */
    private int queryMaxYearlySerial(String year) {
        QueryWrapper<orders> queryWrapper = new QueryWrapper<>();
        // 订单ID格式：SO202500001，前缀为"SO+年份"
        queryWrapper.likeRight("id", "SO" + year);
        // 截取流水号部分（从第6位开始："SO2025"是5位）
        queryWrapper.select("MAX(SUBSTRING(id, 7)) as maxSerial");

        List<Map<String, Object>> result = ordersMapper.selectMaps(queryWrapper);
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
     * 生成订单ID：SO + 年份（YYYY） + 5位流水号（如SO202500001）
     */
    private String generateOrderId() {
        String currentYear = getCurrentYearStr();
        orderLock.lock(); // 与发货单保持一致的锁机制
        try {
            // 跨年时重置流水号（与跨天逻辑对齐）
            if (!currentYear.equals(currentYearStr)) {
                int maxSerial = queryMaxYearlySerial(currentYear);
                currentYearStr = currentYear;
                yearlySerial.set(maxSerial > 0 ? maxSerial : 1);
            }

            // 生成序号后校验是否存在，确保唯一（与发货单逻辑完全一致）
            int serial;
            String orderId;
            do {
                serial = yearlySerial.getAndIncrement();
                orderId = String.format("SO%s%05d", currentYear, serial);
            } while (checkOrderIdExists(orderId)); // 循环检查直到ID不存在

            return orderId;
        } finally {
            orderLock.unlock();
        }
    }

    /**
     * 检查订单ID是否已存在于数据库（与发货单校验逻辑一致）
     */
    private boolean checkOrderIdExists(String orderId) {
        // 调用mapper查询该ID是否存在
        return ordersMapper.selectById(orderId) != null;
    }


    @Override
    public PageResult<orders> getOrders(OrderQueryParam queryParam) {
        LambdaQueryWrapper<orders> queryWrapper = new LambdaQueryWrapper<>();

        // 综合搜索：同时匹配订单编号和客户名称
        if (StringUtils.hasText(queryParam.getSearch())) {
            String searchTerm = queryParam.getSearch();
            queryWrapper.and(wrapper -> wrapper
                    .like(orders::getId, searchTerm)
                    .or()
                    .like(orders::getCustomerName, searchTerm)
            );
        }

        // 订单状态筛选
        if (StringUtils.hasText(queryParam.getStatus())) {
            queryWrapper.eq(orders::getStatus, queryParam.getStatus());
        }

        // 按创建时间降序排序
        queryWrapper.orderByDesc(orders::getCreatedAt);

        // 分页处理
        IPage<orders> page = new Page<>(
                queryParam.getPageIndex(),
                queryParam.getPageSize()
        );

        IPage<orders> resultPage = this.page(page, queryWrapper);

        // 计算总页数
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

        // 4. 生成订单ID
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

        // 7. 设置未开票
        order.setHasInvoice(false);

        // 8. 保存订单
        baseMapper.insert(order);
        // 记录活动日志
        ActivityService activityService = SpringContextHolder.getBean(ActivityService.class);
        activityService.recordActivity(
                "新订单创建",
                "订单号：" + order.getId(),
                "订单管理",
                "green"
        );

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
        existingOrder.setHasInvoice(false);

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


    //获取已完成订单列表（支持分页和筛选）
    @Override
    public DeliveredOrdersResponse getDeliveredOrders(Integer pageIndex, Integer pageSize, String orderId, String status,Boolean hasInvoice) {
        // 1. 处理分页参数默认值
        int defaultPageIndex = 0;
        int defaultPageSize = 10;
        pageIndex = (pageIndex == null || pageIndex < 0) ? defaultPageIndex : pageIndex;
        pageSize = (pageSize == null || pageSize <= 0) ? defaultPageSize : pageSize;

        // 2. 核心查询条件：status为"已完成"
        QueryWrapper<orders> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "已完成");

        // 3. 订单编号模糊搜索
        if (orderId != null && !orderId.trim().isEmpty()) {
            queryWrapper.like("id", orderId.trim());
        }
        if (hasInvoice != null) {
            // 如果hasInvoice为true，筛选出有发票的订单；为false则筛选无发票的订单
            queryWrapper.eq("hasInvoice", hasInvoice);
        }

        // 4. 执行分页查询
        Page<orders> page = new Page<>(pageIndex + 1, pageSize);
        IPage<orders> orderPage = ordersMapper.selectPage(page, queryWrapper);
        List<orders> orderList = orderPage.getRecords();
        long total = orderPage.getTotal();

        // 5. 关联发货单（保持不变）
        List<delivery_order_items> deliveryItems = deliveryOrderItemsMapper.selectList(null);
        Map<String, String> orderIdToDeliveryIdMap = deliveryItems.stream()
                .filter(item -> item.getOrderId() != null && !item.getOrderId().isEmpty())
                .collect(Collectors.toMap(
                        delivery_order_items::getOrderId,
                        delivery_order_items::getDeliveryOrderId,
                        (existing, replacement) -> existing
                ));

        List<delivery_orders> allDeliveries = deliveryOrdersMapper.selectList(null);
        Map<String, delivery_orders> deliveryIdToDeliveryMap = allDeliveries.stream()
                .collect(Collectors.toMap(
                        delivery_orders::getId,
                        delivery -> delivery,
                        (existing, replacement) -> existing
                ));

        // 新增：查询发票并建立订单ID→发票的映射
        List<invoices> allInvoices = invoicesMapper.selectList(null);
        Map<String, invoices> orderIdToInvoiceMap = allInvoices.stream()
                .filter(invoice -> invoice.getOrderId() != null && !invoice.getOrderId().isEmpty())
                .collect(Collectors.toMap(
                        invoices::getOrderId,
                        invoice -> invoice,
                        (existing, replacement) -> existing
                ));

        // 6. 构建响应数据（修改后）
        List<DeliveredOrdersResponse.OrderItem> orderItems = orderList.stream()
                .map(order -> {
                    DeliveredOrdersResponse.OrderItem item = new DeliveredOrdersResponse.OrderItem();
                    item.setId(order.getId());
                    item.setCustomerId(order.getCustomerId());
                    item.setCustomerName(order.getCustomerName());
                    item.setAmount(order.getTotalAmount());
                    item.setOrderDate(order.getCreatedAt());
                    item.setStatus("已完成");
                    item.setHasInvoice(order.isHasInvoice());

                    // 补充发货日期
                    String deliveryOrderId = orderIdToDeliveryIdMap.get(order.getId());
                    if (deliveryOrderId != null) {
                        delivery_orders deliveryOrder = deliveryIdToDeliveryMap.get(deliveryOrderId);
                        if (deliveryOrder != null) {
                            item.setDeliveryDate(deliveryOrder.getDeliveryDate());
                        }
                    }

                    // 动态设置invoiceId（关键修改）
                    if (order.isHasInvoice()) {
                        invoices matchedInvoice = orderIdToInvoiceMap.get(order.getId());
                        item.setInvoiceId(matchedInvoice != null ? matchedInvoice.getInvoiceId() : null);
                    } else {
                        item.setInvoiceId(null);
                    }

                    return item;
                })
                .collect(Collectors.toList());

        // 7. 构建最终响应
        DeliveredOrdersResponse response = new DeliveredOrdersResponse();
        response.setData(orderItems);
        response.setTotal(total);
        response.setPageCount((int) Math.ceil((double) total / pageSize));

        return response;
    }

    // 获取状态为已发货&已完成的订单列表
    @Override
    public OrderPageResponseDTO getInprocessOrders(String status, int page, int pageSize, String search) {
        // 1. 构建分页对象
        Page<orders> orderPage = new Page<>(page, pageSize);

        // 2. 解析状态参数（已发货,已完成）
        String[] statusArr = status.split(",");

        // 3. 构建查询条件
        LambdaQueryWrapper<orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(orders::getStatus, statusArr); // 状态筛选

        // 4. 搜索条件（订单号/发货单号/客户名称）
        if (search != null && !search.isEmpty()) {
            queryWrapper.and(w -> w
                    .like(orders::getId, search) // 订单号
                    .or().like(orders::getCustomerName, search) // 客户名称
            );
        }

        // 5. 执行分页查询
        Page<orders> resultPage = ordersMapper.selectPage(orderPage, queryWrapper);

        // 6. 转换为响应DTO
        OrderPageResponseDTO response = new OrderPageResponseDTO();
        OrderPageResponseDTO.OrderPageData data = new OrderPageResponseDTO.OrderPageData();
        data.setTotal(resultPage.getTotal());
        data.setPage(page);
        data.setPage_size(pageSize);

        // 7. 转换订单列表（关联发货单）
        data.setOrders(resultPage.getRecords().stream().map(order -> {
            OrderItemDTO dto = new OrderItemDTO();
            dto.setId(order.getId());
            dto.setCustomerName(order.getCustomerName());
            dto.setProductName(order.getProductName());
            dto.setQuantity(order.getQuantity());
            dto.setAmount(order.getTotalAmount());
            dto.setOrderDate(order.getCreatedAt()); // 下单时间
            dto.setStatus(order.getStatus());

            // 查询关联的发货单ID（通过 delivery_order_items 中间表）
            LambdaQueryWrapper<delivery_order_items> itemQuery = new LambdaQueryWrapper<>();
            itemQuery.eq(delivery_order_items::getOrderId, order.getId());
            delivery_order_items deliveryOrderItem = deliveryOrderItemsMapper.selectOne(itemQuery);

            if (deliveryOrderItem != null) {
                // 通过发货单ID查询发货单
                LambdaQueryWrapper<delivery_orders> deliveryQuery = new LambdaQueryWrapper<>();
                deliveryQuery.eq(delivery_orders::getId, deliveryOrderItem.getDeliveryOrderId());
                delivery_orders deliveryOrder = deliveryOrdersMapper.selectOne(deliveryQuery);

                if (deliveryOrder != null) {
                    dto.setDeliveryOrderId(String.valueOf(deliveryOrder.getId())); // 发货单号
                }
            }

            return dto;
        }).collect(Collectors.toList()));

        response.setData(data);
        return response;
    }

    // 修改订单状态（仅支持"已发货"→"已完成"）
    @Override
    @Transactional
    public OrderPageResponseDTO updateOrderStatus(String orderId, OrderStatusUpdateDTO updateDTO) {
        OrderPageResponseDTO response = new OrderPageResponseDTO();

        // 1. 校验新状态是否为"已完成"
        if (!"已完成".equals(updateDTO.getStatus())) {
            response.setCode(400);
            response.setMessage("无效的状态值");
            return response;
        }

        // 2. 查询订单是否存在
        orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            response.setCode(404);
            response.setMessage("订单不存在");
            return response;
        }

        // 3. 校验当前状态是否为"已发货"
        if (!"已发货".equals(order.getStatus())) {
            response.setCode(400);
            response.setMessage("仅支持已发货状态更新为已完成");
            return response;
        }

        // 4. 更新订单状态
        order.setStatus("已完成");
        ordersMapper.updateById(order);

        // 5. 返回成功响应
        response.setMessage("状态更新成功");
        return response;
    }
}

