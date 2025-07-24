package org.example.erp.dto;

import java.util.List;

public class PageResult<T> {
    private List<T> orders; // 订单列表
    private long total;     // 总记录数
    private int pageCount;  // 总页数

    public PageResult(List<T> orders, long total, int pageCount) {
        this.orders = orders;
        this.total = total;
        this.pageCount = pageCount;
    }

    // Getters
    public List<T> getOrders() {
        return orders;
    }

    public long getTotal() {
        return total;
    }

    public int getPageCount() {
        return pageCount;
    }
}