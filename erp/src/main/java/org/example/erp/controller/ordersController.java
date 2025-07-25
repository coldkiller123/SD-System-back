package org.example.erp.controller;

import org.example.erp.dto.OrderCreateDTO;
import org.example.erp.dto.OrderQueryParam;
import org.example.erp.dto.OrderUpdateDTO;
import org.example.erp.dto.PageResult;
import org.example.erp.entity.orders;
import org.example.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<orders> updateOrder(
            @PathVariable String id, // 订单ID从路径获取，确保不可修改
            @RequestBody OrderUpdateDTO updateDTO) {
        orders updatedOrder = orderService.updateOrder(id, updateDTO);
        return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
    }
}