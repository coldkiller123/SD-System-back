package org.example.erp.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import static com.baomidou.mybatisplus.annotation.IdType.AUTO;

/**
 * 联系人实体类
 * 对应数据库contacts表，存储联系人相关信息
 */
@TableName("contacts")
public class contacts {
    // 主键ID，自增
    @TableId(type = AUTO)
    private int id;

    // 联系人姓名
    private String name;

    // 联系人邮箱
    private String email;

    // 联系人电话
    private String phone;

    // 联系人职位
    private String position;

    // getter和setter方法
    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}