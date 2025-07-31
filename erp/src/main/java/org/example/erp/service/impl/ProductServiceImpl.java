package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.erp.dto.ProductDTO;
import org.example.erp.entity.products;
import org.example.erp.mapper.productsMapper;
import org.example.erp.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private productsMapper productsMapper;

    @Override
    public List<ProductDTO> searchProducts(String keyword) {
        // 构建查询条件
        QueryWrapper<products> queryWrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 模糊匹配产品ID或名称
            queryWrapper.and(wrapper -> {
                wrapper.like("id", keyword.trim())
                        .or()
                        .like("name", keyword.trim());
            });
        }

        // 查询产品列表
        List<products> productList = productsMapper.selectList(queryWrapper);

        // 转换为DTO并返回
        return productList.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private ProductDTO convertToDTO(products product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getUnitPrice());  // 映射单价
        dto.setStock(product.getQuantity());   // 映射库存
        return dto;
    }
}
