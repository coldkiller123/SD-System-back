package org.example.erp.service.impl;

import org.example.erp.entity.attachments;
import org.example.erp.mapper.attachmentsMapper;
import org.example.erp.service.attachmentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;

@Service
public class attachmentsServiceImpl implements attachmentsService {

    @Autowired
    private attachmentsMapper attachmentsMapper;

    // 从配置文件读取上传根路径
    @Value("${file.upload.base-path:/uploads}")
    private String baseUploadPath;

    @Override
    public attachments getAttachmentById(Integer id) {
        return attachmentsMapper.selectById(id);
    }

    @Override
    public List<attachments> getAttachmentsByCustomerId(String customerId) {
        QueryWrapper<attachments> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("customerId", customerId);
        return attachmentsMapper.selectList(queryWrapper);
    }

    @Override
    public void previewAttachment(Integer id, HttpServletResponse response) throws IOException {
        // 1. 获取附件信息
        attachments attachment = getAttachmentById(id);
        if (attachment == null) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "附件不存在");
            return;
        }

        String fullPath = baseUploadPath + File.separator + attachment.getFilePath();
        File file = new File(fullPath);


        System.out.println("baseUploadPath: " + baseUploadPath);
        System.out.println("attachment.getFilePath(): " + attachment.getFilePath());

        // 3. 验证文件是否存在
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "baseUploadPath: " +fullPath);
            return;
        }

        // 4. 设置响应头
        response.setContentType(getMimeType(file.getName()));
        String fileName = URLEncoder.encode(attachment.getFileName(), "UTF-8");
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        response.setContentLengthLong(file.length());

        // 5. 写入文件流到响应
        try (FileInputStream in = new FileInputStream(file);
             BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream())) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    // 根据文件名获取MIME类型
    private String getMimeType(String fileName) {
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".doc")) return "application/msword";
        if (fileName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (fileName.endsWith(".xls")) return "application/vnd.ms-excel";
        if (fileName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }


    // 新增：通过 filepath 直接预览文件（核心实现）
    @Override
    public void previewByFilepath(String filepath, HttpServletResponse response) throws IOException {
        // 1. 拼接完整文件路径（baseUploadPath + filepath）
        File file = new File(baseUploadPath, filepath);
        // 注意：File 构造方法会自动处理分隔符，无需手动拼接

        // 2. 验证文件是否存在
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "文件不存在或已被删除");
            return;
        }

        // 3. 从 filepath 中提取文件名（用于响应头）
        String filename = new File(filepath).getName();

        // 4. 设置响应头（指定文件类型、文件名编码）
        response.setContentType(getMimeType(filename));
        // 解决中文文件名乱码问题
        String encodedFilename = URLEncoder.encode(filename, "UTF-8");
        response.setHeader("Content-Disposition", "inline; filename=\"" + encodedFilename + "\"");
        response.setContentLengthLong(file.length()); // 设置文件大小

        // 5. 写入文件流到响应
        try (FileInputStream in = new FileInputStream(file);
             BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream())) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush(); // 确保所有数据写入响应
        } catch (IOException e) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件读取失败");
            throw new RuntimeException("预览文件时发生IO错误", e);
        }
    }

    // 删除方法
    @Transactional
    @Override
    public boolean deleteByFilepath(String filepath) {
        File file = new File(baseUploadPath, filepath);
        if (!file.exists() || !file.isFile()) {
            throw new RuntimeException("文件不存在：" + filepath);
        }

        QueryWrapper<attachments> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("filepath", filepath); // 按文件路径匹配数据库记录
        int deleteCount = attachmentsMapper.delete(queryWrapper);

        if (deleteCount == 0) {
            throw new RuntimeException("数据库中未找到该附件记录：" + filepath);
        }

        boolean isDeleted = file.delete();
        if (!isDeleted) {
            throw new RuntimeException("本地文件删除失败：" + filepath);
        }

        return true;
    }

}
