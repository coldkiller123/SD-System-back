package org.example.erp.dto;

public class OrderQueryParam {
    private Integer pageIndex = 0; // 当前页码（从0开始）
    private Integer pageSize = 10; // 每页数量，默认10
    private String search;         // 综合搜索：订单编号、客户名称等
    private String status;         // 订单状态

    // Getters and Setters
    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
