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

    @NotBlank(message = "用户名不能为空")
    private String username;  // 关联的用户名

    @NotBlank(message = "请求ID不能为空")
    private String requestId;  // 发送验证码时返回的requestId
}
