package org.example.erp.controller;

import org.example.erp.service.attachmentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;



@RestController
@RequestMapping("/attachments")
public class attachmentsController {

    @Autowired
    private attachmentsService attachmentsService;

    // 正确的接口写法：HttpServletResponse 作为响应对象，无需任何注解
    @GetMapping("/preview")
    public void previewAttachment(
            @RequestParam("id") Integer id,  // 仅对业务参数加 @RequestParam
            HttpServletResponse response     // 响应对象直接声明，不加任何注解
    ) {
        try {
            attachmentsService.previewAttachment(id, response);
        } catch (IOException e) {
            try {
                response.sendError(500, "预览失败：" + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @GetMapping("/preview-by-filepath")
    public void previewByFilepath(
            @RequestParam("filepath") String filepath,
            HttpServletResponse response               
    ) throws IOException {
        // 直接调用服务层的previewByFilepath方法，传递路径和响应对象
        attachmentsService.previewByFilepath(filepath, response);
    }

    // 删除接口（使用ResponseEntity响应）
    @DeleteMapping("/delete-by-filepath")
    public ResponseEntity<?> deleteByFilepath(@RequestParam("filepath") String filepath) {
        try {
            boolean isDeleted = attachmentsService.deleteByFilepath(filepath);
            return ResponseEntity.ok().body(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "附件删除成功");
                put("data", isDeleted);
            }});
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
}
