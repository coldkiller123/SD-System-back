package org.example.erp.controller;

import org.example.erp.dto.InquiryCreateDTO;
import org.example.erp.entity.inquiries;
import org.example.erp.service.InquiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
