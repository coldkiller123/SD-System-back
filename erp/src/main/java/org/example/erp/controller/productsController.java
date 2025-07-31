package org.example.erp.controller;

import org.example.erp.dto.ProductDTO;
import org.example.erp.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class productsController {

    @Autowired
    private ProductService productService;

    //产品查询接口，支持关键词搜索
    @GetMapping("/products")
    public List<ProductDTO> getProducts(@RequestParam(required = false) String keyword) {
        return productService.searchProducts(keyword);
    }
}
