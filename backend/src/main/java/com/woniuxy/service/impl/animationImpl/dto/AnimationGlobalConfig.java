package com.woniuxy.service.impl.animationImpl.dto;

import lombok.Data;

@Data
public class AnimationGlobalConfig {
    /**
     * 渲染帧率
     */
    private Integer fps;
    /**
     * 画布背景色
     */
    private String background;
    /**
     * 是否循环播放
     */
    private Boolean loop;
    /**
     * 预留全局自定义配置
     */
    private Object customConfig;
}