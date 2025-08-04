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

        // 3. 查询所有相关模块的活动日志（只关注四个模块，移除销售订单）
        LambdaQueryWrapper<ActivityLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ActivityLog::getModule, "客户管理", "订单管理", "发货管理", "财务管理")
                .orderByDesc(ActivityLog::getCreateTime);
        List<ActivityLog> logs = activityLogMapper.selectList(queryWrapper);

        // 4. 按模块分组，只保留每个模块最新的一条记录
        Map<String, ActivityLog> latestLogs = new HashMap<>();
        for (ActivityLog log : logs) {
            String module = log.getModule();
            // 只处理四个指定模块，不涉及销售订单
            if ("客户管理".equals(module) || "订单管理".equals(module) ||
                    "发货管理".equals(module) || "财务管理".equals(module)) {
                if (!latestLogs.containsKey(module)) {
                    latestLogs.put(module, log);
                }
            }
        }

        // 5. 转换为DTO列表（按固定顺序：客户管理→订单管理→发货管理→财务管理）
        List<ActivityListResponseDTO.ActivityDTO> activityDTOs = new ArrayList<>();

        // 客户管理
        activityDTOs.add(buildActivityDTO(
                latestLogs.get("客户管理"),
                "客户管理",
                "客户总数",
                "Users",
                "blue",
                customerCount,
                todayNewCustomers
        ));

        // 订单管理（只保留订单管理，无销售订单）
        activityDTOs.add(buildActivityDTO(
                latestLogs.get("订单管理"),
                "订单管理",
                "订单总数",
                "FileText",
                "green",
                orderCount,
                todayNewOrders
        ));

        // 发货管理
        activityDTOs.add(buildActivityDTO(
                latestLogs.get("发货管理"),
                "发货管理",
                "发货单总数",
                "Package",
                "purple",
                deliveryCount,
                todayNewDeliveries
        ));

        // 财务管理
        activityDTOs.add(buildActivityDTO(
                latestLogs.get("财务管理"),
                "财务管理",
                "发票总数",
                "Receipt",
                "red",
                invoiceCount,
                todayNewInvoices
        ));

        // 6. 构建响应
        ActivityListResponseDTO response = new ActivityListResponseDTO();
        ActivityListResponseDTO.ActivityData data = new ActivityListResponseDTO.ActivityData();
        data.setActivities(activityDTOs);
        response.setData(data);
        response.setCode(200);
        response.setMessage("success");

        return response;
    }

    // 构建单个活动DTO的工具方法
    private ActivityListResponseDTO.ActivityDTO buildActivityDTO(
            ActivityLog log,
            String module,
            String titleSta,
            String icon,
            String color,
            long totalValue,
            long todayNew) {

        ActivityListResponseDTO.ActivityDTO dto = new ActivityListResponseDTO.ActivityDTO();
        dto.setModule(module);
        dto.setTitleSta(titleSta);
        dto.setIcon(icon);
        dto.setColor(color);
        dto.setValue((int) totalValue);
        dto.setTodayNew("今日新增 + " + todayNew);

        if (log != null) {
            dto.setTitleAct(log.getTitleAct());
            dto.setDescription(log.getDescription() + " - " + formatTimeDifference(log.getCreateTime()));
        } else {
            dto.setTitleAct("无新增");
            dto.setDescription("无新增记录");
        }

        return dto;
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

        // 按模块设置默认icon（只处理四个模块）
        switch (module) {
            case "客户管理":
                log.setIcon("Users");
                break;
            case "订单管理":
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
