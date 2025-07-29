package org.example.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.erp.dto.*;
import org.example.erp.entity.order_histories;
import org.example.erp.entity.orders;
import org.example.erp.dto.DeliveredOrdersResponse;

public interface OrderService extends IService<orders> {
    PageResult<orders> getOrders(OrderQueryParam queryParam);
    orders createOrder(OrderCreateDTO orderDTO);
    orders updateOrder(String orderId, OrderUpdateDTO updateDTO);
    order_histories getLatestHistory(String orderId);
    OrderDetailDTO getOrderDetail(String orderId);
    UnshippedOrderResponseDTO getUnshippedOrders(UnshippedOrderQueryDTO queryDTO);

    //查询已收货订单列表（支持分页和筛选）
    DeliveredOrdersResponse getDeliveredOrders (Integer pageIndex, Integer pageSize, String orderId, String status);

    // 获取状态为已发货&已完成的订单列表
    OrderPageResponseDTO getInprocessOrders(String status, int page, int pageSize, String search);

    // 修改订单状态（仅支持"已发货"→"已完成"）
    OrderPageResponseDTO updateOrderStatus(String orderId, OrderStatusUpdateDTO updateDTO);
}
