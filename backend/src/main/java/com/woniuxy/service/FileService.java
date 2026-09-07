package com.woniuxy.service;

import com.woniuxy.utils.ResponseResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * 头像上传
     * @param file 上传文件
     * @return OSS图片url
     */
    ResponseResult uploadAvatar(MultipartFile file);
}