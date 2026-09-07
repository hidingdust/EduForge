package com.woniuxy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "game_record")
public class GameRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "works_id")
    private Long worksId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "score")
    private Integer score;

    @Column(name = "total_question")
    private Integer totalQuestion;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "is_pass")
    private Boolean isPass;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // 可选：新增自动填充创建时间
    @PrePersist
    public void initCreateTime() {
        this.createTime = LocalDateTime.now();
    }
}