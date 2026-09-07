package com.woniuxy.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserChangePwdDTO {
    @NotBlank(message = "旧密码不能为空")
    private String oldPwd;
    @NotBlank(message = "新密码不能为空")
    private String newPwd;
    private String confirmPwd;
}