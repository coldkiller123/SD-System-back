package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.erp.dto.DeliveryOrderCreateDTO;
import org.example.erp.dto.DeliveryOrderResponseDTO;
import org.example.erp.entity.delivery_order_items;
import org.example.erp.entity.delivery_orders;
import org.example.erp.entity.products;
import org.example.erp.entity.orders;
import org.example.erp.mapper.delivery_order_itemsMapper;
import org.example.erp.mapper.delivery_ordersMapper;
import org.example.erp.mapper.ordersMapper;
import org.example.erp.mapper.productsMapper;
import org.example.erp.service.DeliveryOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryOrderServiceImpl extends ServiceImpl<delivery_ordersMapper, delivery_orders> implements DeliveryOrderService {

    @Autowired
    private delivery_ordersMapper deliveryOrdersMapper;

    @Autowired
    private delivery_order_itemsMapper deliveryOrderItemsMapper;

    @Autowired
    private ordersMapper ordersMapper;

    @Autowired
    private productsMapper productsMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 事务管理：任何异常回滚
    public DeliveryOrderResponseDTO createDeliveryOrder(DeliveryOrderCreateDTO createDTO) {
        try {
            // 1. 校验订单是否存在且状态为“已付款”
            List<String> orderIds = createDTO.getOrder_ids();
            for (String orderId : orderIds) {
                orders order = ordersMapper.selectById(orderId);
                if (order == null) {
                    return DeliveryOrderResponseDTO.fail("订单不存在：" + orderId, 400);
                }
                if (!"已付款".equals(order.getStatus())) {
                    return DeliveryOrderResponseDTO.fail("订单" + orderId + "状态不是“已付款”，无法发货", 400);
                }
            }

            // 2. 校验库存是否充足（按商品维度汇总需发货数量）
            for (String orderId : orderIds) {
                orders order = ordersMapper.selectById(orderId);
                String productId = order.getProductId();
                BigDecimal requiredQuantity = order.getQuantity(); // 该订单需发货数量

                // 查询商品当前库存
                LambdaQueryWrapper<products> productQuery = new LambdaQueryWrapper<>();
                productQuery.eq(products::getId, productId);
                products product = productsMapper.selectOne(productQuery);
                if (product == null) {
                    return DeliveryOrderResponseDTO.fail("商品不存在：" + productId, 400);
                }

                BigDecimal stockQuantity = product.getQuantity(); // 当前库存
                if (stockQuantity.compareTo(requiredQuantity) < 0) {
                    // 库存不足
                    return DeliveryOrderResponseDTO.fail(
                            "商品" + product.getName() + "库存不足（当前：" + stockQuantity + "，需：" + requiredQuantity + "）",
                            400
                    );
                }
            }

            // 3. 创建发货单主表记录
            delivery_orders deliveryOrder = new delivery_orders();
            deliveryOrder.setDeliveryDate(createDTO.getDeliveryDate());
            deliveryOrder.setWarehouseManager(createDTO.getWarehouseManager());
            deliveryOrder.setRemarks(createDTO.getRemarks());
            deliveryOrdersMapper.insert(deliveryOrder); // 插入后自动生成ID（AUTO自增）

            // 获取自动生成的发货单ID（int类型）
            int deliveryOrderId = deliveryOrder.getId();
            String deliveryOrderIdStr = String.valueOf(deliveryOrderId); // 转为字符串用于关联

            // 4. 创建发货单明细表记录（关联订单与发货单）
            for (String orderId : orderIds) {
                delivery_order_items item = new delivery_order_items();
                item.setDeliveryOrderId(deliveryOrderIdStr); // 关联发货单
                item.setOrderId(orderId); // 关联订单
                deliveryOrderItemsMapper.insert(item);
            }

            // 5. 扣减库存（按订单商品扣减）
            for (String orderId : orderIds) {
                orders order = ordersMapper.selectById(orderId);
                String productId = order.getProductId();
                BigDecimal requiredQuantity = order.getQuantity();

                // 查询商品并扣减库存
                products product = productsMapper.selectById(productId);
                BigDecimal newStock = product.getQuantity().subtract(requiredQuantity);
                product.setQuantity(newStock);
                productsMapper.updateById(product);
            }

            // 6. 更新订单状态为“已发货”
            for (String orderId : orderIds) {
                orders order = ordersMapper.selectById(orderId);
                order.setStatus("已发货");
                ordersMapper.updateById(order);
            }

            // 7. 返回成功响应（发货单ID）
            return DeliveryOrderResponseDTO.success(deliveryOrderIdStr);

        } catch (Exception e) {
            // 捕获异常并返回服务器错误
            return DeliveryOrderResponseDTO.fail("服务器内部错误：" + e.getMessage(), 500);
        }
    }
}