package org.example.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.erp.dto.*;
import org.example.erp.entity.order_histories;
import org.example.erp.entity.orders;

public interface OrderService extends IService<orders> {
    PageResult<orders> getOrders(OrderQueryParam queryParam);
    orders createOrder(OrderCreateDTO orderDTO);
    orders updateOrder(String orderId, OrderUpdateDTO updateDTO);
    order_histories getLatestHistory(String orderId);
    OrderDetailDTO getOrderDetail(String orderId);
    UnshippedOrderResponseDTO getUnshippedOrders(UnshippedOrderQueryDTO queryDTO);
}
