package com.woniuxy.controller;

import com.woniuxy.service.UserSignInService;
import com.woniuxy.utils.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class Upload1Controller {

    @Resource
    private UserSignInService userSignInService;

    @PostMapping("/file")
    public ResponseResult uploadFile(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {

        Integer userId = userSignInService.getLoginUserId(request);
        if (userId == null) {
            return ResponseResult.fail("未登录");
        }

        try {
            // 上传逻辑...
            Integer resourceId = 1; // 从上传结果获取

            int newScore = userSignInService.uploadAddScore(userId, resourceId);
            return ResponseResult.success("上传成功，积分 +10", newScore);

        } catch (Exception e) {
            return ResponseResult.fail("上传失败：" + e.getMessage());
        }
    }
}