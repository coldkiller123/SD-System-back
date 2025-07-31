package org.example.erp.service;

import org.example.erp.dto.ProductDTO;
import java.util.List;

public interface ProductService {

    List<ProductDTO> searchProducts(String keyword);
}
