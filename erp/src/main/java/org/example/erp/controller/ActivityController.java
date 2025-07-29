package org.example.erp.controller; // 确保此包在主启动类的扫描范围内

import org.example.erp.dto.ActivityListResponseDTO;
import org.example.erp.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 必须添加此注解，标识为REST接口控制器
@RequestMapping("/api/activities") // 基础路径，与请求路径匹配
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    // 接口路径：/api/activities/latest（基础路径+方法路径）
    @GetMapping("/latest")
    public ResponseEntity<ActivityListResponseDTO> getLatestActivities() {
        ActivityListResponseDTO response = activityService.getLatestActivities();
        return ResponseEntity.ok(response);
    }
}