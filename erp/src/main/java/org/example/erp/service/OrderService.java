package org.example.erp.service;

import org.example.erp.dto.OrderQueryParam;
import org.example.erp.dto.PageResult;
import org.example.erp.entity.orders;

public interface OrderService {
    PageResult<orders> getOrders(OrderQueryParam queryParam);
}