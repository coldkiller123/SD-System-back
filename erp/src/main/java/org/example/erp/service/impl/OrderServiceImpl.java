package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.erp.dto.OrderQueryParam;
import org.example.erp.dto.PageResult;
import org.example.erp.entity.orders;
import org.example.erp.mapper.ordersMapper;
import org.example.erp.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderServiceImpl extends ServiceImpl<ordersMapper, orders> implements OrderService {

    @Override
    public PageResult<orders> getOrders(OrderQueryParam queryParam) {
        // 1. 构建查询条件
        LambdaQueryWrapper<orders> queryWrapper = new LambdaQueryWrapper<>();

        // 订单ID模糊查询
        if (StringUtils.hasText(queryParam.getOrderId())) {
            queryWrapper.like(orders::getId, queryParam.getOrderId());
        }

        // 客户名称模糊查询
        if (StringUtils.hasText(queryParam.getCustomerName())) {
            queryWrapper.like(orders::getCustomerName, queryParam.getCustomerName());
        }

        // 状态精确查询 (根据数据库ENUM类型匹配)
        if (StringUtils.hasText(queryParam.getStatus())) {
            queryWrapper.eq(orders::getStatus, queryParam.getStatus());
        }

        // 按创建时间倒序
        queryWrapper.orderByDesc(orders::getCreatedAt);

        // 2. 创建分页对象
        IPage<orders> page = new Page<>(
                queryParam.getPageIndex(),
                queryParam.getPageSize()
        );

        // 3. 执行分页查询
        IPage<orders> resultPage = this.page(page, queryWrapper);

        // 4. 计算总页数
        int pageCount = (int) Math.ceil((double) resultPage.getTotal() / queryParam.getPageSize());

        // 5. 返回结果
        return new PageResult<>(
                resultPage.getRecords(),
                resultPage.getTotal(),
                pageCount
        );
    }
}