package org.example.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.erp.dto.OrderCreateDTO;
import org.example.erp.dto.OrderQueryParam;
import org.example.erp.dto.OrderUpdateDTO;
import org.example.erp.dto.PageResult;
import org.example.erp.entity.orders;

public interface OrderService extends IService<orders> {
    PageResult<orders> getOrders(OrderQueryParam queryParam);
    orders createOrder(OrderCreateDTO orderDTO);
    orders updateOrder(String orderId, OrderUpdateDTO updateDTO);
}
