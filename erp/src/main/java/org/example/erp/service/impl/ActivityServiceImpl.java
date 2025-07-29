package org.example.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        // 1. 查询最新30条活动日志
        LambdaQueryWrapper<ActivityLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ActivityLog::getCreateTime)
                .last("LIMIT 30");
        List<ActivityLog> logs = activityLogMapper.selectList(queryWrapper);

        // 2. 转换活动日志为DTO（恢复时间差拼接）
        List<ActivityListResponseDTO.ActivityDTO> activityDTOs = logs.stream().map(log -> {
            ActivityListResponseDTO.ActivityDTO dto = new ActivityListResponseDTO.ActivityDTO();
            dto.setTitle(log.getTitle());
            // 关键：调用 formatTimeDifference 方法并拼接结果
            dto.setDescription(log.getDescription() + " - " + formatTimeDifference(log.getCreateTime()));
            dto.setModule(log.getModule());
            dto.setColor(log.getColor());
            return dto;
        }).collect(Collectors.toList());

        // 3. 计算各实体总数
        long orderCount = ordersMapper.selectCount(null);
        long customerCount = customersMapper.selectCount(null);
        long deliveryCount = deliveryOrdersMapper.selectCount(null);
        long invoiceCount = invoicesMapper.selectCount(null);

        // 4. 封装总数DTO
        ActivityListResponseDTO.Totals totals = new ActivityListResponseDTO.Totals();
        totals.setOrderCount(orderCount);
        totals.setCustomerCount(customerCount);
        totals.setDeliveryCount(deliveryCount);
        totals.setInvoiceCount(invoiceCount);

        // 5. 构建响应
        ActivityListResponseDTO response = new ActivityListResponseDTO();
        ActivityListResponseDTO.ActivityData data = new ActivityListResponseDTO.ActivityData();
        data.setActivities(activityDTOs);
        data.setTotals(totals);
        response.setData(data);

        return response;
    }

    @Override
    public void recordActivity(String title, String description, String module, String color) {
        ActivityLog log = new ActivityLog();
        log.setTitle(title);
        log.setDescription(description);
        log.setModule(module);
        log.setColor(color);
        log.setCreateTime(LocalDateTime.now());
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
