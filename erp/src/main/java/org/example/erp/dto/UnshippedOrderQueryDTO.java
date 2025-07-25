package org.example.erp.dto;

public class UnshippedOrderQueryDTO {
    private int page; // 当前页码（必须，默认1）
    private int page_size = 10; // 每页数量（默认10）
    private String search; // 搜索关键字（可选）

    // Getters and Setters
    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPage_size() {
        return page_size;
    }

    public void setPage_size(int page_size) {
        this.page_size = page_size;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
}