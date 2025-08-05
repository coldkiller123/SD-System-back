package org.example.erp.controller;

import org.example.erp.dto.ApiResponse;
import org.example.erp.dto.SendEmailCodeRequest;
import org.example.erp.dto.VerifyEmailCodeRequest;
import org.example.erp.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private JdbcTemplate jdbcTemplate;  // 用于操作数据库

    // 验证码配置
    private static final int CODE_LENGTH = 6;  // 修改为6位数字验证码，更安全
    private static final int EXPIRE_MINUTES = 10;  // 有效期延长至10分钟
    private static final int REQUEST_INTERVAL_SECONDS = 60;  // 60秒内限制发送

    /**
     * 发送邮箱验证码接口
     */
    @PostMapping("/send-email-code")
    @Transactional
    public ApiResponse<EmailCodeResponseData> sendEmailCode(
            @Validated @RequestBody SendEmailCodeRequest request
    ) {
        String email = request.getEmail();
        String username = request.getUsername();
        String type = request.getType(); // 获取验证码类型
        Date now = new Date();

        // 1. 验证验证码类型
        if (!"forgot_password".equals(type)) {
            return ApiResponse.fail("不支持的验证码类型");
        }

        // 2. 校验用户合法性
        if (!isValidUser(username)) {
            return ApiResponse.fail("用户名不存在");
        }

        // 3. 创建临时表（会话级，自动销毁）
        jdbcTemplate.execute("""
            CREATE TEMPORARY TABLE IF NOT EXISTS temp_email_captcha (
                id VARCHAR(36) PRIMARY KEY,
                request_id VARCHAR(36) NOT NULL UNIQUE, -- 新增request_id用于验证关联
                email VARCHAR(100) NOT NULL,
                username VARCHAR(50) NOT NULL,
                code VARCHAR(10) NOT NULL,
                type VARCHAR(20) NOT NULL, -- 验证码类型
                create_time DATETIME NOT NULL,
                expire_time DATETIME NOT NULL,
                is_used TINYINT NOT NULL DEFAULT 0
            )
        """);

        // 4. 限制发送频率（60秒内只能发送一次）
        Date beforeTime = new Date(now.getTime() - REQUEST_INTERVAL_SECONDS * 1000);
        Integer recentCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM temp_email_captcha
                WHERE email = ? AND username = ? AND type = ? AND create_time >= ?
            """, Integer.class, email, username, type, beforeTime);

        if (recentCount != null && recentCount > 0) {
            return ApiResponse.fail("发送过于频繁，请1分钟后再试");
        }

        // 5. 生成验证码和requestId
        String code = generateNumericCode(CODE_LENGTH);
        Date expireTime = new Date(now.getTime() + EXPIRE_MINUTES * 60 * 1000);
        String id = UUID.randomUUID().toString();
        String requestId = UUID.randomUUID().toString(); // 生成requestId

        // 6. 保存到临时表
        jdbcTemplate.update("""
                INSERT INTO temp_email_captcha 
                (id, request_id, email, username, code, type, create_time, expire_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, id, requestId, email, username, code, type, now, expireTime);

        // 7. 发送邮件
        try {
            emailService.sendCaptchaEmail(email, username, code, EXPIRE_MINUTES);
        } catch (Exception e) {
            // 发送失败回滚
            jdbcTemplate.update("DELETE FROM temp_email_captcha WHERE id = ?", id);
            return ApiResponse.fail("验证码发送失败，请稍后重试");
        }

        // 8. 构建响应
        EmailCodeResponseData data = new EmailCodeResponseData();
        data.setEmail(desensitizeEmail(email));
        data.setExpireTime(EXPIRE_MINUTES * 60);
        data.setRequestId(requestId); // 返回requestId
        return ApiResponse.success("验证码已发送至邮箱，10分钟内有效", data);
    }

    /**
     * 验证邮箱验证码接口
     */
    @PostMapping("/verify-email-code")
    @Transactional
    public ApiResponse<VerifyResponseData> verifyEmailCode(
            @Validated @RequestBody VerifyEmailCodeRequest request
    ) {
        String email = request.getEmail();
        String code = request.getCode();
        String username = request.getUsername();
        String requestId = request.getRequestId(); // 获取请求ID
        Date now = new Date();

        // 1. 验证必要参数
        if (requestId == null || requestId.trim().isEmpty()) {
            return ApiResponse.fail("请求ID不能为空");
        }

        // 2. 查询有效验证码
        String sql = """
            SELECT code FROM temp_email_captcha
            WHERE email = ? AND username = ? AND request_id = ? AND type = 'forgot_password' 
            AND is_used = 0 AND expire_time > ?
        """;

        String storedCode;
        try {
            storedCode = jdbcTemplate.queryForObject(sql, String.class,
                    email, username, requestId, now);
        } catch (Exception e) {
            return ApiResponse.fail("验证码错误或已过期");
        }

        // 3. 验证验证码
        if (!code.equals(storedCode)) {
            return ApiResponse.fail("验证码错误或已过期");
        }

        // 4. 标记为已使用
        jdbcTemplate.update("""
                UPDATE temp_email_captcha
                SET is_used = 1
                WHERE email = ? AND username = ? AND request_id = ? AND code = ?
            """, email, username, requestId, code);

        // 5. 生成验证令牌
        VerifyResponseData data = new VerifyResponseData();
        data.setVerifyToken(generateVerifyToken(email, username, requestId));
        return ApiResponse.success("验证码验证通过", data);
    }

    // 生成6位数字验证码
    private String generateNumericCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    // 邮箱脱敏处理
    private String desensitizeEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) return email;
        String prefix = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return prefix.charAt(0) + "***" + prefix.charAt(prefix.length() - 1) + domain;
    }

    // 生成验证令牌
    private String generateVerifyToken(String email, String username, String requestId) {
        // 实际项目中可使用JWT生成令牌，包含过期时间
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 验证用户合法性（实际项目中需查询用户表）
    private boolean isValidUser(String username) {
        // 示例：查询用户表是否存在该用户
        // 实际SQL：SELECT COUNT(*) FROM users WHERE username = ?
        return username != null && !username.trim().isEmpty();
    }

    // 发送验证码响应数据
    public static class EmailCodeResponseData {
        private String email;
        private int expireTime;
        private String requestId;  // 新增requestId字段

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public int getExpireTime() { return expireTime; }
        public void setExpireTime(int expireTime) { this.expireTime = expireTime; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
    }

    // 验证验证码响应数据
    public static class VerifyResponseData {
        private String verifyToken;

        public String getVerifyToken() { return verifyToken; }
        public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }
    }
}
