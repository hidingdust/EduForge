package com.woniuxy.controller;

import com.woniuxy.service.UserSignInService;
import com.woniuxy.utils.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/download")
public class Download1Controller {

    @Resource
    private UserSignInService userSignInService;

    @GetMapping("/file")
    public ResponseResult downloadFile(
            HttpServletRequest request,
            @RequestParam Integer resourceId) {

        Integer userId = userSignInService.getLoginUserId(request);
        if (userId == null) {
            return ResponseResult.fail("未登录");
        }

        try {
            int newScore = userSignInService.downloadDeductScore(userId, resourceId);
            if (newScore < 0) {
                return ResponseResult.fail("积分不足，下载需要5积分，当前积分：" + userSignInService.getUserScore(userId));
            }

            // 下载逻辑...
            return ResponseResult.success("下载成功，扣除5积分", newScore);

        } catch (Exception e) {
            return ResponseResult.fail("下载失败：" + e.getMessage());
        }
    }
}