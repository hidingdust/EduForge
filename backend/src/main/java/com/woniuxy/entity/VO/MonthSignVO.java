package com.woniuxy.entity.VO;

import lombok.Data;
import java.util.Set;

@Data
public class MonthSignVO {
    private Integer year;
    private Integer month;
    private Integer totalDays;           // 该月签到总天数
    private Set<Integer> signDays;       // 该月签到日期（日）
}