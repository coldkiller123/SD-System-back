package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.erp.dto.ActivityListResponseDTO;
import org.example.erp.entity.ActivityLog;
import org.example.erp.entity.customers;
import org.example.erp.entity.delivery_orders;
import org.example.erp.entity.invoices;
import org.example.erp.entity.orders;
import org.example.erp.mapper.ActivityLogMapper;
import org.example.erp.mapper.customersMapper;
import org.example.erp.mapper.delivery_ordersMapper;
import org.example.erp.mapper.invoicesMapper;
import org.example.erp.mapper.ordersMapper;
import org.example.erp.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityLogMapper, ActivityLog> implements ActivityService {

    @Autowired
    private ActivityLogMapper activityLogMapper;

    @Autowired
    private ordersMapper ordersMapper;

    @Autowired
    private customersMapper customersMapper;

    @Autowired
    private delivery_ordersMapper deliveryOrdersMapper;

    @Autowired
    private invoicesMapper invoicesMapper;

    @Override
    public ActivityListResponseDTO getLatestActivities() {
        // 1. 计算各实体总数（用于value字段）
        long orderCount = ordersMapper.selectCount(null);
        long customerCount = customersMapper.selectCount(null);
        long deliveryCount = deliveryOrdersMapper.selectCount(null);
        long invoiceCount = invoicesMapper.selectCount(null);

        // 2. 计算今日新增数量（todayNew字段）
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long todayNewOrders = getTodayNewCount(ordersMapper, orders::getCreatedAt, todayStart);
        long todayNewCustomers = getTodayNewCount(customersMapper, customers::getCreatedAt, todayStart);
        long todayNewDeliveries = getTodayNewCount(deliveryOrdersMapper, delivery_orders::getDeliveryDate, todayStart);
        long todayNewInvoices = getTodayNewCount(invoicesMapper, invoices::getIssueDate, todayStart);

        // 3. 查询所有相关模块的活动日志（只关注我们需要的四类）
        LambdaQueryWrapper<ActivityLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ActivityLog::getModule, "客户管理", "订单管理", "销售订单", "发货管理", "财务管理")
                .orderByDesc(ActivityLog::getCreateTime);
        List<ActivityLog> logs = activityLogMapper.selectList(queryWrapper);

        // 4. 按模块分组，只保留每个模块最新的一条记录
        Map<String, ActivityLog> latestLogs = new HashMap<>();
        for (ActivityLog log : logs) {
            String module = log.getModule();
            // 对于"销售订单"，统一归到"订单管理"组
            String groupKey = "销售订单".equals(module) ? "订单管理" : module;

            // 如果该组还没有记录，或者有更新的记录，则替换
            if (!latestLogs.containsKey(groupKey)) {
                latestLogs.put(groupKey, log);
            }
        }

        // 5. 转换为DTO列表
        List<ActivityListResponseDTO.ActivityDTO> activityDTOs = latestLogs.values().stream().map(log -> {
            ActivityListResponseDTO.ActivityDTO dto = new ActivityListResponseDTO.ActivityDTO();
            String module = log.getModule();
            String groupKey = "销售订单".equals(module) ? "订单管理" : module;

            // 设置各模块对应的统计信息
            switch (groupKey) {
                case "客户管理":
                    dto.setTitleAct(log.getTitleAct());
                    dto.setTitleSta("客户总数");
                    dto.setIcon("Users");
                    dto.setValue((int) customerCount);
                    dto.setTodayNew((int) todayNewCustomers);
                    break;
                case "订单管理":
                    dto.setTitleAct(log.getTitleAct());
                    dto.setTitleSta("订单总数");
                    dto.setIcon("FileText");
                    dto.setValue((int) orderCount);
                    dto.setTodayNew((int) todayNewOrders);
                    break;
                case "发货管理":
                    dto.setTitleAct(log.getTitleAct());
                    dto.setTitleSta("发货单总数");
                    dto.setIcon("Package");
                    dto.setValue((int) deliveryCount);
                    dto.setTodayNew((int) todayNewDeliveries);
                    break;
                case "财务管理":
                    dto.setTitleAct(log.getTitleAct());
                    dto.setTitleSta("发票总数");
                    dto.setIcon("Receipt");
                    dto.setValue((int) invoiceCount);
                    dto.setTodayNew((int) todayNewInvoices);
                    break;
                default:
                    dto.setTitleAct(log.getTitleAct());
                    dto.setTitleSta("其他");
                    dto.setIcon("");
                    dto.setValue(0);
                    dto.setTodayNew(0);
            }

            dto.setDescription(log.getDescription() + " - " + formatTimeDifference(log.getCreateTime()));
            dto.setModule(log.getModule());
            dto.setColor(log.getColor());

            return dto;
        }).collect(Collectors.toList());

        // 6. 构建响应
        ActivityListResponseDTO response = new ActivityListResponseDTO();
        ActivityListResponseDTO.ActivityData data = new ActivityListResponseDTO.ActivityData();
        data.setActivities(activityDTOs);
        response.setData(data);

        return response;
    }

    // 抽取通用方法：计算今日新增数量
    private <T> long getTodayNewCount(
            BaseMapper<T> mapper,
            SFunction<T, ?> timeField,
            LocalDateTime todayStart) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(timeField, todayStart);
        return mapper.selectCount(wrapper);
    }

    @Override
    public void recordActivity(String title, String description, String module, String color) {
        ActivityLog log = new ActivityLog();
        log.setTitleAct(title);
        log.setTitleSta(title);
        log.setDescription(description);
        log.setModule(module);
        log.setColor(color);
        log.setCreateTime(LocalDateTime.now());

        // 按模块设置默认icon
        switch (module) {
            case "客户管理":
                log.setIcon("Users");
                break;
            case "订单管理":
            case "销售订单":
                log.setIcon("FileText");
                break;
            case "发货管理":
                log.setIcon("Package");
                break;
            case "财务管理":
                log.setIcon("Receipt");
                break;
            default:
                log.setIcon("");
        }

        activityLogMapper.insert(log);
    }

    /**
     * 格式化时间差
     */
    private String formatTimeDifference(LocalDateTime createTime) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(createTime, now);

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return "刚刚";
        }

        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + "分钟前";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "小时前";
        }

        long days = duration.toDays();
        return days + "天前";
    }
}
