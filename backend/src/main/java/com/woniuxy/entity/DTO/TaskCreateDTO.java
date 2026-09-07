package com.woniuxy.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class TaskCreateDTO {

    //用户上传文档的需求
    private String userRequirements;

    //文档地址
    @NotBlank(message = "文档地址不能为空")
    private String docUrl;
}
