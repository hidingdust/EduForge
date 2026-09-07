package com.woniuxy.service.impl.animationImpl.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnimationMetadata {
    private String title;
    private String description;
    /**
     * 动画总时长（毫秒）
     */
    private Long totalDuration;
    private String author;
    private LocalDateTime createdAt;
}

