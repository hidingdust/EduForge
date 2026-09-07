package com.woniuxy.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EduResourcePublishDTO {
    @NotBlank(message = "资源标题不能为空")
    @Size(max = 60)
    private String title;

    @Size(max = 350)
    private String description;

    @NotBlank(message = "请选择分区")
    private String categoryId;

    // 封面：单个文件
    private MultipartFile coverImage;

    //【改造前-多文件数组，保留注释方便回溯】
    // private MultipartFile[] resourceFiles;

    //【改造后：仅支持单个资源文件】
    private MultipartFile resourceFile;
}