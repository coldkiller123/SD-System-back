package org.example.erp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;  // 发送者邮箱

    /**
     * 发送验证码邮件
     */
    public void sendCaptchaEmail(String to, String username, String code, int expireMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("【邮箱验证】您的验证码");
        message.setText(String.format(
                "尊敬的用户 %s，您好！\n" +
                        "您的邮箱验证码为：%s\n" +
                        "该验证码 %d 分钟内有效，请尽快完成验证。\n" +
                        "请勿将验证码泄露给他人。",
                username, code, expireMinutes
        ));
        mailSender.send(message);
    }
}
