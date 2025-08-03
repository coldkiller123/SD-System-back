package org.example.erp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailCodeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;  // 接收验证码的邮箱

    @NotBlank(message = "验证码不能为空")
    private String code;  // 用户输入的验证码

    private String username;  // 关联的用户名（可选）
}
