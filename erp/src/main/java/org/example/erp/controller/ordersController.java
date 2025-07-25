package org.example.erp.controller;

import org.example.erp.dto.OrderCreateDTO;
import org.example.erp.dto.OrderQueryParam;
import org.example.erp.dto.OrderUpdateDTO;
import org.example.erp.dto.PageResult;
import org.example.erp.entity.order_histories;
import org.example.erp.entity.orders;
import org.example.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class ordersController {

    private final OrderService orderService;

    @Autowired
    public ordersController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public PageResult<orders> getOrders(OrderQueryParam queryParam) {
        return orderService.getOrders(queryParam);
    }

    @PostMapping
    public ResponseEntity<orders> createOrder(@RequestBody OrderCreateDTO orderDTO) {
        orders createdOrder = orderService.createOrder(orderDTO);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

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
}