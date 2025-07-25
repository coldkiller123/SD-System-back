package org.example.erp.dto;
import java.util.List;
import org.example.erp.entity.inquiries;
public class InquiryPageResult {
    private List<inquiries> inquiries; // 询价单列表
    private int total; // 总记录数
    private int pageCount; // 总页数

    // Getters and Setters
    public List<inquiries> getInquiries() {
        return inquiries;
    }

    public void setInquiries(List<inquiries> inquiries) {
        this.inquiries = inquiries;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
}