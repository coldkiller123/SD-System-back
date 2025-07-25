package org.example.erp.dto;

public class DeliveryOrderResponseDTO {
    // 成功响应字段
    private int code;
    private String message;
    private String deliveryOrderId; // 发货单ID（成功时返回）

    // 失败响应字段
    private String error;
    private int status;

    // 成功响应构造方法
    public static DeliveryOrderResponseDTO success(String deliveryOrderId) {
        DeliveryOrderResponseDTO dto = new DeliveryOrderResponseDTO();
        dto.setCode(200);
        dto.setMessage("成功");
        dto.setDeliveryOrderId(deliveryOrderId);
        return dto;
    }

    // 失败响应构造方法
    public static DeliveryOrderResponseDTO fail(String error, int status) {
        DeliveryOrderResponseDTO dto = new DeliveryOrderResponseDTO();
        dto.setError(error);
        dto.setStatus(status);
        return dto;
    }

    // Getters and Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeliveryOrderId() {
        return deliveryOrderId;
    }

    public void setDeliveryOrderId(String deliveryOrderId) {
        this.deliveryOrderId = deliveryOrderId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}