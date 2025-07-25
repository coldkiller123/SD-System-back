package org.example.erp.controller;

import org.example.erp.dto.DeliveryOrderCreateDTO;
import org.example.erp.dto.DeliveryOrderResponseDTO;
import org.example.erp.service.DeliveryOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery-orders")
public class delivery_ordersController {

    @Autowired
    private DeliveryOrderService deliveryOrderService;

    /**
     * 创建发货单接口
     * 接收前端传递的创建发货单相关数据，调用业务层方法处理创建逻辑
     * @param createDTO 包含要发货的订单ID数组、发货备注、发货日期、仓库管理员姓名等信息的DTO对象
     * @return 响应实体，根据业务处理结果返回对应的状态码和响应内容
     */
    @PostMapping
    public ResponseEntity<DeliveryOrderResponseDTO> createDeliveryOrder(
            @RequestBody DeliveryOrderCreateDTO createDTO) {
        // 调用业务层的创建发货单方法，获取处理结果
        DeliveryOrderResponseDTO response = deliveryOrderService.createDeliveryOrder(createDTO);
        // 根据响应结果中的状态码，返回对应的 HTTP 状态码
        if (response.getCode() == 200) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}