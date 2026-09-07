package com.woniuxy.service;

import java.util.Map;

public interface GoldService {

    /**
     * 获取金币
     */
    Map<String, Integer> getGold(Integer userId);

    /**
     * 充值
     */
    Map<String, Object> recharge(Integer userId, Double amount, String paymentMethod);

    /**
     * 金币换积分
     */
    Map<String, Object> exchange(Integer userId, Integer gold);
}