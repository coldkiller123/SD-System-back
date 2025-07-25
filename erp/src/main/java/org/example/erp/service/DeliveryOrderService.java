package org.example.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.erp.dto.DeliveryOrderCreateDTO;
import org.example.erp.dto.DeliveryOrderResponseDTO;
import org.example.erp.entity.delivery_orders;

public interface DeliveryOrderService extends IService<delivery_orders> {
    // 创建发货单
    DeliveryOrderResponseDTO createDeliveryOrder(DeliveryOrderCreateDTO createDTO);
}