package org.example.erp.controller;

import org.example.erp.dto.InquiryCreateDTO;
import org.example.erp.dto.InquiryPageResult;
import org.example.erp.dto.InquiryQueryParam;
import org.example.erp.dto.InquiryStatusUpdateDTO;
import org.example.erp.entity.inquiries;
import org.example.erp.service.InquiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiries")
public class inquiriesController {

    private final InquiryService inquiryService;

    @Autowired
    public inquiriesController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    /**
     * 创建询价单接口
     * POST /api/inquiries
     */
    @PostMapping
    public ResponseEntity<inquiries> createInquiry(@RequestBody InquiryCreateDTO inquiryDTO) {
        inquiries createdInquiry = inquiryService.createInquiry(inquiryDTO);
        return new ResponseEntity<>(createdInquiry, HttpStatus.CREATED);
    }
    /**
     * 询价单列表接口（支持分页、搜索、筛选）
     * GET /api/inquiries
     */
    @GetMapping
    public ResponseEntity<InquiryPageResult> getInquiries(InquiryQueryParam queryParam) {
        InquiryPageResult result = inquiryService.getInquiries(queryParam);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
    /**
     * 7. 更新询价单状态接口
     * PUT /api/inquiries/{inquiryId}/status
     */
    @PutMapping("/{inquiryId}/status")
    public ResponseEntity<inquiries> updateInquiryStatus(
            @PathVariable String inquiryId,
            @RequestBody InquiryStatusUpdateDTO updateDTO) {
        inquiries updatedInquiry = inquiryService.updateInquiryStatus(inquiryId, updateDTO);
        return new ResponseEntity<>(updatedInquiry, HttpStatus.OK);
    }
}
