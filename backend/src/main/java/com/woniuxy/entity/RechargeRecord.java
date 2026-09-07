package com.woniuxy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recharge_record")
public class RechargeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "gold", nullable = false)
    private Integer gold;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "status")
    private Integer status;  // 0-失败 1-成功

    @Column(name = "create_time")
    private String createTime;

    @Column(name = "update_time")
    private String updateTime;
}