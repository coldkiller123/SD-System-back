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
import java.util.HashMap;
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

            // 生成序号后校验是否存在，确保唯一
            int serial;
            String deliveryOrderId;
            do {
                serial = dailySerial.getAndIncrement();
                deliveryOrderId = String.format("DEL%s-%03d", today, serial);
            } while (checkDeliveryOrderIdExists(deliveryOrderId));

            return deliveryOrderId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检查发货单号是否已存在于数据库
     */
    private boolean checkDeliveryOrderIdExists(String deliveryOrderId) {
        return deliveryOrdersMapper.selectById(deliveryOrderId) != null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeliveryOrderResponseDTO createDeliveryOrder(DeliveryOrderCreateDTO createDTO) {
        try {
            List<String> orderIds = createDTO.getOrder_ids();

            // 1. 校验所有订单是否存在且状态为“已付款”
            for (String orderId : orderIds) {
                orders order = ordersMapper.selectById(orderId);
                if (order == null) {
                    return DeliveryOrderResponseDTO.fail("订单不存在：" + orderId, 400);
                }
                if (!"已付款".equals(order.getStatus())) {
                    return DeliveryOrderResponseDTO.fail("订单" + orderId + "状态不是“已付款”，无法发货", 400);
                }
            }

            // 2. 按商品维度汇总所有订单的需求量（核心修改）
            Map<String, BigDecimal> productTotalQuantity = new HashMap<>();
            for (String orderId : orderIds) {
                orders order = ordersMapper.selectById(orderId);
                String productId = order.getProductId();
                BigDecimal quantity = order.getQuantity();

                // 累加同一商品的总需求量
                productTotalQuantity.put(
                        productId,
                        productTotalQuantity.getOrDefault(productId, BigDecimal.ZERO).add(quantity)
                );
            }

            // 3. 校验库存是否充足（使用汇总后的需求量）
            for (Map.Entry<String, BigDecimal> entry : productTotalQuantity.entrySet()) {
                String productId = entry.getKey();
                BigDecimal totalRequired = entry.getValue(); // 同一商品的总需求量

                // 查询商品库存
                products product = productsMapper.selectById(productId);
                if (product == null) {
                    return DeliveryOrderResponseDTO.fail("商品不存在：" + productId, 400);
                }

                BigDecimal stockQuantity = product.getQuantity();
                if (stockQuantity.compareTo(totalRequired) < 0) {
                    return DeliveryOrderResponseDTO.fail(
                            "商品" + product.getName() + "库存不足（当前：" + stockQuantity + "，需：" + totalRequired + "）",
                            400
                    );
                }
            }

            // 4. 生成发货单并保存
            String deliveryOrderId = generateDeliveryOrderId();
            delivery_orders deliveryOrder = new delivery_orders();
            deliveryOrder.setId(deliveryOrderId);
            deliveryOrder.setDeliveryDate(createDTO.getDeliveryDate());
            deliveryOrder.setWarehouseManager(createDTO.getWarehouseManager());
            deliveryOrder.setRemarks(createDTO.getRemarks());
            deliveryOrdersMapper.insert(deliveryOrder);

            // 5. 创建明细记录
            for (String orderId : orderIds) {
                delivery_order_items item = new delivery_order_items();
                item.setDeliveryOrderId(deliveryOrderId);
                item.setOrderId(orderId);
                deliveryOrderItemsMapper.insert(item);
            }

            // 6. 扣减库存
            for (Map.Entry<String, BigDecimal> entry : productTotalQuantity.entrySet()) {
                String productId = entry.getKey();
                BigDecimal totalRequired = entry.getValue();

                products product = productsMapper.selectById(productId);
                product.setQuantity(product.getQuantity().subtract(totalRequired));
                productsMapper.updateById(product);
            }

            // 7. 更新订单状态
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
                    "发货管理",
                    "orange"
            );

            return DeliveryOrderResponseDTO.success(deliveryOrderId);

        } catch (Exception e) {
            return DeliveryOrderResponseDTO.fail("服务器内部错误：" + e.getMessage(), 500);
        }
    }
}
