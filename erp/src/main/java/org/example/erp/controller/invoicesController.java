package org.example.erp.controller;

import org.example.erp.dto.InvoiceGenerateResponse;
import org.example.erp.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice")
public class invoicesController {

    @Autowired
    private InvoiceService invoiceService;

    //根据订单ID生成发票
    @PostMapping("/generate/{orderId}")
    public InvoiceGenerateResponse generateInvoice(@PathVariable String orderId) {
        return invoiceService.generateInvoice(orderId);
    }
}
