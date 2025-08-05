package org.example.erp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendEmailCodeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;  // 用户邮箱

    @NotBlank(message = "用户名不能为空")
    private String username;  // 关联的用户名

    @NotBlank(message = "验证码类型不能为空")
    private String type;  // 验证码类型，如：forgot_password
}
