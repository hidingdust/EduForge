package com.woniuxy.entity.VO;

import lombok.Data;
import java.util.Set;

@Data
public class SignInfoVO {
    private Integer totalScore;          // 总积分
    private Boolean signedToday;         // 今日是否签到
    private Integer monthSignDays;       // 本月签到天数
    private Set<Integer> signDays;       // 本月签到日期（日）
    private Integer continuousDays;      // 连续签到天数
}