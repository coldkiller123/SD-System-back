package org.example.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.erp.dto.InquiryCreateDTO;
import org.example.erp.entity.inquiries;

public interface InquiryService extends IService<inquiries> {
    inquiries createInquiry(InquiryCreateDTO inquiryDTO);
}
