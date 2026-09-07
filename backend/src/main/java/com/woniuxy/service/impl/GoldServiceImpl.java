package com.woniuxy.service.impl;

import com.woniuxy.entity.PO.UserPO;
import com.woniuxy.entity.RechargeRecord;
import com.woniuxy.repository.RechargeRecordRepository;
import com.woniuxy.repository.UserRepository;
import com.woniuxy.service.GoldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class GoldServiceImpl implements GoldService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RechargeRecordRepository rechargeRecordRepository;

    @Override
    public Map<String, Integer> getGold(Integer userId) {
        UserPO user = userRepository.findById(userId).orElse(new UserPO());
        Map<String, Integer> result = new HashMap<>();
        result.put("gold", user.getGold() != null ? user.getGold() : 0);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> recharge(Integer userId, Double amount, String paymentMethod) {
        if (amount == null || amount <= 0) {
            throw new RuntimeException("金额无效");
        }

        // 1元=10金币
        int gold = (int) (amount * 10);

        // 更新金币
        userRepository.addGold(userId, gold);

        // 记录充值
        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setGold(gold);
        record.setPaymentMethod(paymentMethod);
        record.setStatus(1);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        rechargeRecordRepository.save(record);

        // 返回结果
        UserPO user = userRepository.findById(userId).orElseThrow();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newGold", user.getGold());
        result.put("gainGold", gold);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> exchange(Integer userId, Integer gold) {
        if (gold == null || gold <= 0) {
            throw new RuntimeException("金币数量无效");
        }

        // 1金币=1积分
        int rows = userRepository.exchange(userId, gold, gold);
        if (rows == 0) {
            throw new RuntimeException("金币不足");
        }

        UserPO user = userRepository.findById(userId).orElseThrow();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newGold", user.getGold());
        result.put("newScore", user.getScore());
        return result;
    }
}