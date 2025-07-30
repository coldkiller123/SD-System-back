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
import java.util.List;
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
        long todayNewDeliveries = getTodayNewCount(deliveryOrdersMapper, delivery_orders::getDeliveryDate, todayStart); // 假设用deliveryDate
        long todayNewInvoices = getTodayNewCount(invoicesMapper, invoices::getIssueDate, todayStart);

        // 3. 查询最新30条活动日志
        LambdaQueryWrapper<ActivityLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ActivityLog::getCreateTime).last("LIMIT 30");
        List<ActivityLog> logs = activityLogMapper.selectList(queryWrapper);

        // 4. 转换活动日志为DTO（合并统计信息，匹配前端字段）
        List<ActivityListResponseDTO.ActivityDTO> activityDTOs = logs.stream().map(log -> {
            ActivityListResponseDTO.ActivityDTO dto = new ActivityListResponseDTO.ActivityDTO();

            // 核心：设置双标题、图标，与前端一一对应
            switch (log.getModule()) {
                case "客户管理":
                    dto.setTitleAct(log.getTitleAct()); // 活动标题（如"新客户注册"）
                    dto.setTitleSta("客户总数");       // 统计标题（对应stats的title）
                    dto.setIcon("Users");              // 对应图标
                    dto.setValue((int) customerCount);
                    dto.setTodayNew((int) todayNewCustomers);
                    break;
                case "订单管理":
                case "销售订单": // 兼容模块名
                    dto.setTitleAct(log.getTitleAct()); // 如"新订单创建"
                    dto.setTitleSta("订单总数");
                    dto.setIcon("FileText");
                    dto.setValue((int) orderCount);
                    dto.setTodayNew((int) todayNewOrders);
                    break;
                case "发货管理":
                    dto.setTitleAct(log.getTitleAct()); // 如"发货单生成"
                    dto.setTitleSta("发货单总数");
                    dto.setIcon("Package");
                    dto.setValue((int) deliveryCount);
                    dto.setTodayNew((int) todayNewDeliveries);
                    break;
                case "财务管理":
                    dto.setTitleAct(log.getTitleAct()); // 如"发票开具"
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

            // 保留描述和时间差（如"上海科技有限公司 - 2分钟前"）
            dto.setDescription(log.getDescription() + " - " + formatTimeDifference(log.getCreateTime()));
            dto.setModule(log.getModule());
            dto.setColor(log.getColor());

            return dto;
        }).collect(Collectors.toList());

        // 5. 构建响应（仅包含合并后的activity列表）
        ActivityListResponseDTO response = new ActivityListResponseDTO();
        ActivityListResponseDTO.ActivityData data = new ActivityListResponseDTO.ActivityData();
        data.setActivities(activityDTOs);
        response.setData(data);

        return response;
    }

    // 抽取通用方法：计算今日新增数量（简化代码）
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

        // 按模块设置默认icon（与前端对应）
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
                log.setIcon(""); // 默认空字符串（避免null）
        }

        activityLogMapper.insert(log);
    }

    /**
     * 格式化时间差（如：刚刚、2分钟前、1小时前）
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
