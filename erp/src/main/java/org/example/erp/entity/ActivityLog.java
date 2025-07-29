package org.example.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity_logs") // 数据库表名
public class ActivityLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;       // 活动标题（如"新订单创建"）
    private String description; // 活动描述（如"订单号：ORD202507001 - 刚刚"）
    private String module;      // 所属模块（如"订单管理"）
    private String color;       // 颜色标识（如"green"）
    @TableField("create_time")
    private LocalDateTime createTime; // 活动发生时间
}