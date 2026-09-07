package com.woniuxy.service;

import com.woniuxy.entity.VO.MonthSignVO;
import com.woniuxy.entity.VO.SignInfoVO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserSignInService {

    // ========== 签到相关 ==========
    int signIn(Integer userId);

    SignInfoVO getSignInfo(Integer userId);

    MonthSignVO getMonthSignInfo(Integer userId, Integer year, Integer month);

    // ========== 通用积分操作 ==========
    int addScore(Integer userId, Integer score, String reason);

    boolean deductScore(Integer userId, Integer score, String reason);

    boolean hasEnoughScore(Integer userId, Integer requiredScore);

    Integer getUserScore(Integer userId);

    // ========== 上传/下载专用 ==========
    int uploadAddScore(Integer userId, Integer resourceId);

    int downloadDeductScore(Integer userId, Integer resourceId);

    // ========== 从 Token 获取用户 ID ==========
    Integer getLoginUserId(HttpServletRequest request);
}