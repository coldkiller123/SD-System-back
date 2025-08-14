package org.example.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 发货单实体类
 * 对应数据库delivery_orders表，存储发货单的基本信息
 */
@TableName("delivery_orders")
public class delivery_orders {
    // 主键ID，采用UUID自动生成
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    // 发货日期
    private String deliveryDate;

    // 仓库管理员
    private String warehouseManager;

    // 备注信息
    private String remarks;

    // getter和setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
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