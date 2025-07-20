package org.example.erp.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import static com.baomidou.mybatisplus.annotation.IdType.AUTO;

@TableName("delivery_order_items")
public class delivery_order_items {
    @TableId(type = AUTO)
    private int id;
    private String deliveryOrderId;
    private String orderId;

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
