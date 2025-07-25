package org.example.erp.dto;
import java.util.List;

public class DeliveryOrderCreateDTO {

    private List<String> order_ids; // 要发货的订单ID数组

    private String remarks; // 发货备注（最多200字符）


    private String deliveryDate; // 发货日期（YYYY-MM-DD）


    private String warehouseManager; // 仓库管理员姓名

    // Getters and Setters
    public List<String> getOrder_ids() {
        return order_ids;
    }

    public void setOrder_ids(List<String> order_ids) {
        this.order_ids = order_ids;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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
}