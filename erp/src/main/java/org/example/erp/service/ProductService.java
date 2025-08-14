package org.example.erp.service;

import org.example.erp.dto.ProductDTO;
import java.util.List;

public interface ProductService {
//搜索库存列表
    List<ProductDTO> searchProducts(String keyword);
}
