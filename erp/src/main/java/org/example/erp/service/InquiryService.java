package org.example.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.erp.dto.InquiryCreateDTO;
import org.example.erp.dto.InquiryPageResult;
import org.example.erp.dto.InquiryQueryParam;
import org.example.erp.dto.InquiryStatusUpdateDTO;
import org.example.erp.entity.inquiries;

public interface InquiryService extends IService<inquiries> {
    inquiries createInquiry(InquiryCreateDTO inquiryDTO);
    InquiryPageResult getInquiries(InquiryQueryParam queryParam);
    inquiries updateInquiryStatus(String inquiryId, InquiryStatusUpdateDTO updateDTO);
}
