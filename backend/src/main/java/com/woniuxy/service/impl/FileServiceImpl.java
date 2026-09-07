package com.woniuxy.service.impl;

import com.woniuxy.service.FileService;
import com.woniuxy.utils.OssUtil;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final OssUtil ossUtil;

    @Override
    public ResponseResult uploadAvatar(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return new ResponseResult(400, "文件不能为空");
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if(lastDotIndex == -1){
            return new ResponseResult(400, "文件缺少后缀名");
        }
        String suffix = fileName.substring(lastDotIndex);
        if (!suffix.matches("(?i)\\.(jpg|jpeg|png|webp)")) {
            return new ResponseResult(400, "仅支持jpg、jpeg、png、webp格式图片");
        }
        // 限制5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            return new ResponseResult(400, "图片不能超过5MB");
        }
        String url;
        try {
            url = ossUtil.uploadMultipartFile(file, "avatar");
        } catch (Exception e) {
            return new ResponseResult(500, "图片上传OSS失败");
        }
        return new ResponseResult(url);
    }
}