package org.example.erp.controller;

import org.example.erp.dto.OrderQueryParam;
import org.example.erp.dto.PageResult;
import org.example.erp.entity.orders;
import org.example.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}