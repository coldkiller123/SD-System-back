package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
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
import org.example.erp.service.ActivityService;
import org.example.erp.service.DeliveryOrderService;
import org.example.erp.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

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
    // 当天日期（用于判断是否跨天）
    private String currentDate;
    // 当天流水号（原子类保证线程安全）
    private AtomicInteger dailySerial;
    // 锁：解决并发和跨天重置问题
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 初始化：服务启动时加载当天已有最大流水号
     */
    @PostConstruct
    public void init() {
        String today = getCurrentDateStr();
        int maxSerial = queryMaxDailySerial(today);
        currentDate = today;
        dailySerial = new AtomicInteger(maxSerial > 0 ? maxSerial : 1);
    }

    /**
     * 获取当前日期字符串（YYYYMMDD）
     */
    private String getCurrentDateStr() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    /**
     * 查询指定日期的最大流水号
     */
    private int queryMaxDailySerial(String dateStr) {
        QueryWrapper<delivery_orders> queryWrapper = new QueryWrapper<>();
        // 发货单号格式：DEL20250715-003，前缀为"DEL+日期+"
        queryWrapper.likeRight("id", "DEL" + dateStr + "-");
        // 截取流水号部分（从第12位开始："DEL20250715-"是11位）
        queryWrapper.select("MAX(SUBSTRING(id, 12)) as maxSerial");

        List<Map<String, Object>> result = deliveryOrdersMapper.selectMaps(queryWrapper);
        if (result == null || result.isEmpty() || result.get(0) == null) {
            return 0;
        }

        Object maxSerialObj = result.get(0).get("maxSerial");
        if (maxSerialObj == null) {
            return 0;
        }

        try {
            return Integer.parseInt(maxSerialObj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 生成发货单号：DEL + 日期（YYYYMMDD） + "-" + 3位流水号
     */
    private String generateDeliveryOrderId() {
        String today = getCurrentDateStr();
        lock.lock();
        try {
            // 跨天时重置流水号
            if (!today.equals(currentDate)) {
                int maxSerial = queryMaxDailySerial(today);
                currentDate = today;
                dailySerial.set(maxSerial > 0 ? maxSerial : 1);
            }
            // 生成当天流水号（自增）
            int serial = dailySerial.getAndIncrement();
            // 格式化为3位（不足补0）
            return String.format("DEL%s-%03d", today, serial);
        } finally {
            lock.unlock();
        }
    }

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

            // 3. 生成发货单号（关键修改：先生成ID）
            String deliveryOrderId = generateDeliveryOrderId();

            // 创建发货单主表记录（关键修改：设置自定义ID）
            delivery_orders deliveryOrder = new delivery_orders();
            deliveryOrder.setId(deliveryOrderId); // 设置生成的发货单号作为主表ID
            deliveryOrder.setDeliveryDate(createDTO.getDeliveryDate());
            deliveryOrder.setWarehouseManager(createDTO.getWarehouseManager());
            deliveryOrder.setRemarks(createDTO.getRemarks());
            deliveryOrdersMapper.insert(deliveryOrder); // 插入主表

            // 4. 创建发货单明细表记录（关联订单与发货单）
            for (String orderId : orderIds) {
                delivery_order_items item = new delivery_order_items();
                item.setDeliveryOrderId(deliveryOrderId); // 直接使用生成的ID关联
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
            // 记录活动日志
            ActivityService activityService = SpringContextHolder.getBean(ActivityService.class);
            activityService.recordActivity(
                    "新发货单创建",
                    "发货单号：" + deliveryOrderId,
                    "物流管理",
                    "orange"
            );

            // 7. 返回成功响应（发货单ID）
            return DeliveryOrderResponseDTO.success(deliveryOrderId);

        } catch (Exception e) {
            // 捕获异常并返回服务器错误
            return DeliveryOrderResponseDTO.fail("服务器内部错误：" + e.getMessage(), 500);
        }
    }
}