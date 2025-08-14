package org.example.erp.entity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import static com.baomidou.mybatisplus.annotation.IdType.AUTO;

/**
 * 附件实体类
 * 对应数据库attachments表
 */
@TableName("attachments")
public class attachments {
    // 主键ID，自增
    @TableId(type = AUTO)
    private int id;

    // 客户ID，关联所属客户
    private String customerId;

    // 文件名
    private String fileName;

    // 文件存储路径
    private String filePath;

    // getter和setter方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}