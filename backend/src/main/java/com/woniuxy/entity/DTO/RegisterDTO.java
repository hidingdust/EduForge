package com.woniuxy.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位国内手机号")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min=6,max = 16,message = "密码长度6~16位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Size(min=6,max = 16,message = "密码长度6~16位")
    private String confirmPwd;

    @NotBlank(message = "用户名不能为空")
    private String userName;

//    @NotBlank(message = "验证码不能为空")
//    private String code;
}
