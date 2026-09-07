package com.woniuxy.controller;

import com.woniuxy.entity.VO.SignInfoVO;
import com.woniuxy.service.UserSignInService;
import com.woniuxy.utils.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/sign")
public class UserSignInController {

    @Resource
    private UserSignInService userSignInService;

    @PostMapping("/in")
    public ResponseResult signIn(HttpServletRequest request) {
        Integer userId = userSignInService.getLoginUserId(request);
        if (userId == null) {
            return ResponseResult.fail("未登录");
        }
        try {
            int gainScore = userSignInService.signIn(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("gainScore", gainScore);
            data.put("message", "签到成功，获得 " + gainScore + " 积分");
            return ResponseResult.success(data);
        } catch (RuntimeException e) {
            return ResponseResult.fail(e.getMessage());
        }
    }

    @GetMapping("/info")
    public ResponseResult getSignInfo(HttpServletRequest request) {
        Integer userId = userSignInService.getLoginUserId(request);
        if (userId == null) {
            return ResponseResult.fail("未登录");
        }
        SignInfoVO info = userSignInService.getSignInfo(userId);
        return ResponseResult.success(info);
    }

    @GetMapping("/month")
    public ResponseResult getMonthSignInfo(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Integer userId = userSignInService.getLoginUserId(request);
        if (userId == null) {
            return ResponseResult.fail("未登录");
        }

        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();

        com.woniuxy.entity.VO.MonthSignVO vo = userSignInService.getMonthSignInfo(userId, year, month);
        return ResponseResult.success(vo);
    }

    @PostMapping("/uploadScore")
    public ResponseResult uploadAddScore(HttpServletRequest request) {
        Integer userId = userSignInService.getLoginUserId(request);
        if (userId == null) {
            return ResponseResult.fail("未登录");
        }
        try {
            int newScore = userSignInService.uploadAddScore(userId, null);
            Map<String, Object> data = new HashMap<>();
            data.put("gainScore", 10);
            data.put("totalScore", newScore);
            data.put("message", "上传成功，获得 10 积分");
            return ResponseResult.success(data);
        } catch (RuntimeException e) {
            return ResponseResult.fail(e.getMessage());
        }
    }
}