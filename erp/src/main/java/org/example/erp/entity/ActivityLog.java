package org.example.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity_logs") // 与数据库表名保持一致
public class ActivityLog {
    @TableId(type = IdType.AUTO) // 自增主键，与表结构匹配
    private Long id;

    @TableField("titleAct") // 对应表中的titleAct字段
    private String titleAct;

    @TableField("titleSta") // 对应表中的titleSta字段
    private String titleSta;

    @TableField("value") // 对应表中的value字段
    private Integer value;

    @TableField("icon") // 对应表中的icon字段
    private String icon;

    @TableField("todayNew") // 对应表中的todayNew字段
    private Integer todayNew;

    @TableField("description") // 对应表中的description字段
    private String description;

    @TableField("module") // 对应表中的module字段
    private String module;

    @TableField("color") // 对应表中的color字段
    private String color;

    @TableField("create_time") // 对应表中的create_time字段
    private LocalDateTime createTime;
}
