package org.example.erp.service;

import org.example.erp.entity.attachments;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface attachmentsService {

    /**
     * 根据ID获取附件信息
     * @param id 附件ID
     * @return 附件实体
     */
    attachments getAttachmentById(Integer id);

    /**
     * 根据客户ID获取附件列表
     * @param customerId 客户ID
     * @return 附件列表
     */
    List<attachments> getAttachmentsByCustomerId(String customerId);

    /**
     * 预览附件
     * @param id 附件ID
     * @param response HTTP响应对象
     * @throws IOException 处理文件时可能抛出的异常
     */
    void previewAttachment(Integer id, HttpServletResponse response) throws IOException;

    void previewByFilepath(String filepath, HttpServletResponse response) throws IOException;

    boolean deleteByFilepath(String filepath);
}



