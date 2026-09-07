package com.woniuxy.service.impl.DocToPptImpl.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PptTaskDTO {

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @NotBlank(message = "文件base64不能为空")
    private String fileBase64;

    @Size(max = 300, message = "自定义需求不能超过300字")
    private String userDemand;
}