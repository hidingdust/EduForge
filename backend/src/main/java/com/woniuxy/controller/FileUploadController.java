package com.woniuxy.controller;

import com.woniuxy.constant.Auth;
import com.woniuxy.service.FileService;
import com.woniuxy.utils.JWTUtils;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileUploadController {
    private final FileService fileService;

    @PostMapping("/uploadAvatar")
    public ResponseResult uploadAvatar(
            @RequestHeader(Auth.AUTHORIZATION) String token,
            @RequestParam("file") MultipartFile file
    ) {
        // 登录鉴权放在Controller层合理：身份校验属于网关/请求入口校验
        JWTUtils.getUid(token);
        return fileService.uploadAvatar(file);
    }
}