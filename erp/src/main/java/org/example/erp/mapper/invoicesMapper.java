package org.example.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.erp.entity.invoices;

@Mapper
public interface invoicesMapper extends BaseMapper<invoices> {
    // 新增：根据订单ID查询发票（MyBatis-Plus会自动解析SQL）
    invoices selectByOrderId(String orderId);
}