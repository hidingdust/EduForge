package com.woniuxy.service.impl.animationImpl.dto;

import lombok.Data;

@Data
public class AnimationClip {
    /**
     * 动画类型 fadeIn fadeOut zoomIn zoomOut move rotate slideIn slideOut
     */
    private String type;
    /**
     * 动画开始时间（全局时间轴，毫秒）
     */
    private Long startTime;
    /**
     * 持续时长（毫秒）
     */
    private Long duration;
    /**
     * 缓动函数 ease-out / ease-in / ease-in-out / linear
     */
    private String easing;
    /**
     * slide类专用方向；非slide动画禁止输出此字段
     */
    private String direction;
    /**
     * 高级自定义参数
     * move：targetX targetY
     * rotate：angle
     * zoom：scale
     * 所有高级运动参数统一放这里
     */
    private Object customParams;
}