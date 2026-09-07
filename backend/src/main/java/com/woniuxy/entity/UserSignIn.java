package com.woniuxy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_sign_in")
public class UserSignIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "sign_date", nullable = false)
    private LocalDate signDate;

    @Column(name = "gain_score")
    private Integer gainScore = 10;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "month_year")
    private String monthYear; // 格式：2026-08，方便按月查询
}