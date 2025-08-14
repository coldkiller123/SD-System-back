package org.example.erp.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import static com.baomidou.mybatisplus.annotation.IdType.AUTO;

/**
 * 发货单明细实体类
 * 对应数据库delivery_order_items表，存储发货单与原始订单的关联信息
 */
@TableName("delivery_order_items")
public class delivery_order_items {
    // 主键ID，自增
    @TableId(type = AUTO)
    private int id;

    // 发货单ID，关联对应的发货单
    private String deliveryOrderId;

    // 原始订单ID，关联该发货单对应的原始订单
    private String orderId;

    // getter和setter方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeliveryOrderId() {
        return deliveryOrderId;
    }

    public void setDeliveryOrderId(String deliveryOrderId) {
        this.deliveryOrderId = deliveryOrderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}