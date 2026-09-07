package com.woniuxy.service.impl;

import com.woniuxy.entity.PO.UserPO;
import com.woniuxy.entity.User;
import com.woniuxy.entity.UserSignIn;
import com.woniuxy.entity.VO.MonthSignVO;
import com.woniuxy.entity.VO.SignInfoVO;
import com.woniuxy.repository.UserRepository;
import com.woniuxy.repository.UserScoreRepository;
import com.woniuxy.repository.UserSignInRepository;
import com.woniuxy.service.UserSignInService;
import com.woniuxy.utils.JWTUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UserSignInServiceImpl implements UserSignInService {

    // 积分规则常量
    private static final int UPLOAD_SCORE = 10;
    private static final int DOWNLOAD_SCORE = 5;

    @Resource
    private UserSignInRepository userSignInRepository;

    @Resource
    private UserScoreRepository userScoreRepository;

    @Resource
    private UserRepository userRepository;

    // ========== 从 Token 获取登录用户 ID ==========
    @Override
    public Integer getLoginUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            System.out.println("❌ Token 为空");
            return null;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            String phone = JWTUtils.getUid(token);
            System.out.println("✅ 从 Token 解析到手机号: " + phone);

            if (phone == null || phone.isEmpty()) {
                System.out.println("❌ Token 中未包含手机号");
                return null;
            }

            Optional<UserPO> userOpt = userRepository.findByPhone(phone);
            if (userOpt.isEmpty()) {
                System.out.println("❌ 手机号 " + phone + " 对应的用户不存在");
                return null;
            }

            Integer userId = userOpt.get().getUserId();
            System.out.println("✅ 当前登录用户 ID: " + userId);
            return userId;

        } catch (Exception e) {
            System.out.println("❌ Token 解析失败: " + e.getMessage());
            return null;
        }
    }

    // ========== 签到 ==========
    @Override
    @Transactional
    public int signIn(Integer userId) {
        LocalDate today = LocalDate.now();

        Optional<UserSignIn> todaySign = userSignInRepository.findByUserIdAndSignDate(userId, today);
        if (todaySign.isPresent()) {
            throw new RuntimeException("今日已签到");
        }

        int continuousDays = getContinuousDays(userId);
        int gainScore = 10;
        if ((continuousDays + 1) % 7 == 0) {
            gainScore = 20;
        }

        UserSignIn signIn = new UserSignIn();
        signIn.setUserId(userId);
        signIn.setSignDate(today);
        signIn.setGainScore(gainScore);
        signIn.setMonthYear(today.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        signIn.setCreateTime(java.time.LocalDateTime.now());
        userSignInRepository.save(signIn);

        User user = userScoreRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setScore(user.getScore() + gainScore);
        userScoreRepository.save(user);

        return gainScore;
    }

    // ========== 上传资源加分 ==========
    @Override
    @Transactional
    public int uploadAddScore(Integer userId, Integer resourceId) {
        User user = userScoreRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        int newScore = user.getScore() + UPLOAD_SCORE;
        user.setScore(newScore);
        userScoreRepository.save(user);

        System.out.println("✅ 上传资源加分: userId=" + userId + ", resourceId=" + resourceId +
                ", +" + UPLOAD_SCORE + ", 当前积分=" + newScore);

        return newScore;
    }

    // ========== 下载资源扣分 ==========
    @Override
    @Transactional
    public int downloadDeductScore(Integer userId, Integer resourceId) {
        User user = userScoreRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getScore() < DOWNLOAD_SCORE) {
            System.out.println("❌ 下载扣分失败: userId=" + userId + ", 当前积分=" + user.getScore() + ", 需要=" + DOWNLOAD_SCORE);
            return -1;
        }

        int newScore = user.getScore() - DOWNLOAD_SCORE;
        user.setScore(newScore);
        userScoreRepository.save(user);

        System.out.println("✅ 下载资源扣分: userId=" + userId + ", resourceId=" + resourceId +
                ", -" + DOWNLOAD_SCORE + ", 当前积分=" + newScore);

        return newScore;
    }

    // ========== 通用积分操作 ==========
    @Override
    @Transactional
    public int addScore(Integer userId, Integer score, String reason) {
        User user = userScoreRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        int newScore = user.getScore() + score;
        user.setScore(newScore);
        userScoreRepository.save(user);

        System.out.println("✅ 增加积分: +" + score + ", 原因: " + reason + ", 当前积分: " + newScore);
        return newScore;
    }

    @Override
    @Transactional
    public boolean deductScore(Integer userId, Integer score, String reason) {
        User user = userScoreRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getScore() < score) {
            System.out.println("❌ 积分不足: 当前 " + user.getScore() + ", 需要 " + score);
            return false;
        }

        int newScore = user.getScore() - score;
        user.setScore(newScore);
        userScoreRepository.save(user);

        System.out.println("✅ 减少积分: -" + score + ", 原因: " + reason + ", 当前积分: " + newScore);
        return true;
    }

    @Override
    public boolean hasEnoughScore(Integer userId, Integer requiredScore) {
        User user = userScoreRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        return user.getScore() >= requiredScore;
    }

    @Override
    public Integer getUserScore(Integer userId) {
        User user = userScoreRepository.findById(userId).orElse(null);
        return user != null ? user.getScore() : 0;
    }

    // ========== 签到信息 ==========
    @Override
    public SignInfoVO getSignInfo(Integer userId) {
        LocalDate today = LocalDate.now();
        String currentMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        SignInfoVO vo = new SignInfoVO();

        User user = userScoreRepository.findById(userId).orElse(null);
        vo.setTotalScore(user != null ? user.getScore() : 0);
        vo.setSignedToday(userSignInRepository.findByUserIdAndSignDate(userId, today).isPresent());

        Integer monthDays = userSignInRepository.countByUserIdAndMonthYear(userId, currentMonth);
        vo.setMonthSignDays(monthDays != null ? monthDays : 0);

        List<UserSignIn> monthSigns = userSignInRepository.findByUserIdAndMonthYear(userId, currentMonth);
        Set<Integer> signDays = new HashSet<>();
        for (UserSignIn sign : monthSigns) {
            signDays.add(sign.getSignDate().getDayOfMonth());
        }
        vo.setSignDays(signDays);
        vo.setContinuousDays(getContinuousDays(userId));

        return vo;
    }

    @Override
    public MonthSignVO getMonthSignInfo(Integer userId, Integer year, Integer month) {
        String monthYear = String.format("%04d-%02d", year, month);
        List<UserSignIn> monthSigns = userSignInRepository.findByUserIdAndMonthYear(userId, monthYear);

        MonthSignVO vo = new MonthSignVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setTotalDays(monthSigns.size());

        Set<Integer> signDays = new HashSet<>();
        for (UserSignIn sign : monthSigns) {
            signDays.add(sign.getSignDate().getDayOfMonth());
        }
        vo.setSignDays(signDays);

        return vo;
    }

    // ========== 连续签到天数计算 ==========
    private int getContinuousDays(Integer userId) {
        List<UserSignIn> allSigns = userSignInRepository.findByUserIdOrderBySignDateDesc(userId);
        if (allSigns.isEmpty()) {
            return 0;
        }

        int continuous = 0;
        LocalDate checkDate = LocalDate.now();

        for (UserSignIn sign : allSigns) {
            if (sign.getSignDate().equals(checkDate)) {
                continuous++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }

        return continuous;
    }
}