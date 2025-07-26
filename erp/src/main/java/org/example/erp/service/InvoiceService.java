package org.example.erp.service;

import org.example.erp.dto.InvoiceGenerateResponse;

public interface InvoiceService {
    //根据订单ID生成发票
    InvoiceGenerateResponse generateInvoice(String orderId);
}
