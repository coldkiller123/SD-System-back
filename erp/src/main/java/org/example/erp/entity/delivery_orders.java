package org.example.erp.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import static com.baomidou.mybatisplus.annotation.IdType.AUTO;

@TableName("delivery_orders")
public class delivery_orders {
    @TableId(type = AUTO)
    private int id;
    private String deliveryDate;
    private String warehouseManager;
    private String remarks;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getWarehouseManager() {
        return warehouseManager;
    }

    public void setWarehouseManager(String warehouseManager) {
        this.warehouseManager = warehouseManager;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
