package org.example.erp.controller;

import org.example.erp.dto.*;
import org.example.erp.entity.order_histories;
import org.example.erp.entity.orders;
import org.example.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单管理控制器
 * 处理与订单相关的HTTP请求，包括订单的查询、创建、更新等操作
 */
@RestController
@RequestMapping("/api/orders")
public class ordersController {

    // 订单服务层对象，用于处理订单相关业务逻辑
    private final OrderService orderService;

    /**
     * 构造函数注入OrderService
     * @param orderService 订单服务层对象
     */
    @Autowired
    public ordersController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询订单列表
     * 支持根据查询参数进行筛选查询
     * @param queryParam 订单查询参数对象，包含分页、筛选条件等
     * @return 分页的订单列表结果
     */
    @GetMapping
    public PageResult<orders> getOrders(OrderQueryParam queryParam) {
        return orderService.getOrders(queryParam);
    }

    /**
     * 创建新订单
     * 接收订单创建信息，调用服务层创建订单并返回创建结果
     * @param orderDTO 订单创建数据传输对象，包含订单的基本信息
     * @return 包含创建的订单信息和201状态码的响应实体
     */
    @PostMapping
    public ResponseEntity<orders> createOrder(@RequestBody OrderCreateDTO orderDTO) {
        orders createdOrder = orderService.createOrder(orderDTO);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    /**
     * 更新订单信息
     * 根据订单ID更新订单信息，并返回更新后的订单详情及最新修改记录
     * @param id 订单ID
     * @param updateDTO 订单更新数据传输对象，包含需要更新的字段
     * @return 包含更新后订单信息和200状态码的响应实体
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateOrder(
            @PathVariable String id,
            @RequestBody OrderUpdateDTO updateDTO) {
        // 更新订单
        orders updatedOrder = orderService.updateOrder(id, updateDTO);

        // 查询最新修改记录
        order_histories latestHistory = orderService.getLatestHistory(id);

        // 组合响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("id", updatedOrder.getId());
        response.put("customerId", updatedOrder.getCustomerId());
        response.put("customerName", updatedOrder.getCustomerName());
        response.put("productId", updatedOrder.getProductId());
        response.put("productName", updatedOrder.getProductName());
        response.put("quantity", updatedOrder.getQuantity());
        response.put("unitPrice", updatedOrder.getUnitPrice());
        response.put("totalAmount", updatedOrder.getTotalAmount());
        response.put("paidAmount", updatedOrder.getPaidAmount());
        response.put("status", updatedOrder.getStatus());
        response.put("salesPerson", updatedOrder.getSalesPerson());
        response.put("createdAt", updatedOrder.getCreatedAt());
        response.put("remarks", updatedOrder.getRemarks());

        // 从最新历史记录中获取modifiedAt和modifiedBy
        if (latestHistory != null) {
            response.put("modifiedAt", latestHistory.getModifiedAt());
            response.put("modifiedBy", latestHistory.getModifiedBy());
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 获取订单详情
     * 根据订单ID查询订单的详细信息
     * @param id 订单ID
     * @return 包含订单详情DTO和200状态码的响应实体
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDTO> getOrderDetail(@PathVariable String id) {
        OrderDetailDTO orderDetail = orderService.getOrderDetail(id);
        return new ResponseEntity<>(orderDetail, HttpStatus.OK);
    }

    /**
     * 获取未发货订单列表（状态为“已付款”）
     * 支持分页和搜索功能
     * @param page 页码
     * @param page_size 每页条数，默认为10
     * @param search 搜索关键词，可选
     * @return 包含未发货订单列表的响应实体
     */
    @GetMapping("/unshipped")
    public ResponseEntity<UnshippedOrderResponseDTO> getUnshippedOrders(
            @RequestParam int page,
            @RequestParam(required = false, defaultValue = "10") int page_size,
            @RequestParam(required = false) String search) {

        UnshippedOrderQueryDTO queryDTO = new UnshippedOrderQueryDTO();
        queryDTO.setPage(page);
        queryDTO.setPage_size(page_size);
        queryDTO.setSearch(search);

        UnshippedOrderResponseDTO response = orderService.getUnshippedOrders(queryDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取已完成订单列表
     * 支持分页和多条件筛选（订单ID、是否有发票、状态）
     * @param pageIndex 页码，可选
     * @param pageSize 每页条数，可选
     * @param orderId 订单ID，可选筛选条件
     * @param hasInvoice 是否有发票，可选筛选条件
     * @param status 订单状态，可选筛选条件
     * @return 已完成订单列表响应对象
     */
    @GetMapping ("/delivered")
    public DeliveredOrdersResponse getDeliveredOrders (
            @RequestParam (required = false) Integer pageIndex,
            @RequestParam (required = false) Integer pageSize,
            @RequestParam (required = false) String orderId,
            @RequestParam (required = false) Boolean hasInvoice,
            @RequestParam (required = false) String status) {
        return orderService.getDeliveredOrders (pageIndex, pageSize, orderId, status, hasInvoice);
    }

    /**
     * 获取状态为已发货&已完成的订单列表
     * 支持分页、搜索和状态筛选
     * @param status 订单状态
     * @param page 页码，默认为0
     * @param page_size 每页条数，默认为10
     * @param search 搜索关键词，可选
     * @return 包含符合条件的订单列表的响应实体
     */
    @GetMapping("/inprocess")
    public ResponseEntity<OrderPageResponseDTO> getInprocessOrders(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int page_size,
            @RequestParam(required = false) String search) {

        OrderPageResponseDTO response = orderService.getInprocessOrders(status, page, page_size, search);
        return ResponseEntity.ok(response);
    }

    /**
     * 修改订单状态
     * 根据订单ID更新订单状态，并返回操作结果
     * @param orderId 订单ID
     * @param updateDTO 订单状态更新数据传输对象，包含新状态等信息
     * @return 包含操作结果的响应实体，状态码由响应对象中的code决定
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderPageResponseDTO> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody OrderStatusUpdateDTO updateDTO) {

        OrderPageResponseDTO response = orderService.updateOrderStatus(orderId, updateDTO);
        return ResponseEntity.status(response.getCode()).body(response);
    }

}