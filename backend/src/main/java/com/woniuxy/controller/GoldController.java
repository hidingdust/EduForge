package com.woniuxy.controller;

import com.woniuxy.entity.PO.UserPO;
import com.woniuxy.repository.UserRepository;
import com.woniuxy.service.GoldService;
import com.woniuxy.utils.JWTUtils;
import com.woniuxy.utils.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/gold")
public class GoldController {

    @Autowired
    private GoldService goldService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 从 token 中获取当前用户 ID
     */
    private Integer getCurrentUserId(HttpServletRequest request) {
        // 从请求头获取 token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("未登录");
        }

        try {
            // 从 token 中解析手机号
            String phone = JWTUtils.getUid(token);
            if (phone == null || phone.isEmpty()) {
                throw new RuntimeException("token无效");
            }

            // 根据手机号查询用户
            UserPO user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            return user.getUserId();
        } catch (Exception e) {
            throw new RuntimeException("token无效或已过期");
        }
    }

    /**
     * 获取金币
     */
    @GetMapping("/info")
    public ResponseResult getGold(HttpServletRequest request) {
        Integer userId = getCurrentUserId(request);
        Map<String, Integer> data = goldService.getGold(userId);
        return ResponseResult.success(data);
    }

    /**
     * 充值
     */
    @PostMapping("/recharge")
    public ResponseResult recharge(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer userId = getCurrentUserId(request);
        Double amount = Double.valueOf(params.get("amount").toString());
        String paymentMethod = params.get("paymentMethod").toString();
        Map<String, Object> data = goldService.recharge(userId, amount, paymentMethod);
        return ResponseResult.success(data);
    }

    /**
     * 金币换积分
     */
    @PostMapping("/exchange")
    public ResponseResult exchange(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer userId = getCurrentUserId(request);
        Integer gold = Integer.valueOf(params.get("gold").toString());
        Map<String, Object> data = goldService.exchange(userId, gold);
        return ResponseResult.success(data);
    }
}