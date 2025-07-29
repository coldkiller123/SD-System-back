package org.example.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.erp.dto.ActivityListResponseDTO;
import org.example.erp.entity.ActivityLog;

public interface ActivityService extends IService<ActivityLog> {
    /**
     * 获取最新活动列表及各实体总数
     */
    ActivityListResponseDTO getLatestActivities();

    /**
     * 记录活动日志
     * @param title 活动标题
     * @param description 活动描述
     * @param module 所属模块
     * @param color 颜色标识
     */
    void recordActivity(String title, String description, String module, String color);
}
