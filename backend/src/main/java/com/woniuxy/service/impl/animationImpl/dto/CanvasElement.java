package com.woniuxy.service.impl.animationImpl.dto;

import lombok.Data;

import java.util.List;

@Data
public class CanvasElement {
    /**
     * 全局唯一元素ID，全程不能重复
     */
    private String elementId;

    /**
     * 元素类型：text / shape / line / image
     */
    private String type;

    /**
     * 坐标信息
     */
    private Position position;

    /**
     * 尺寸信息
     */
    private Size size;

    /**
     * 基础样式：透明度、旋转、边框、背景色等
     */
    private ElementStyle style;

    /**
     * 【关键】同一个元素支持多段时序动画数组
     */
    private List<AnimationClip> animations;

    /**
     * 内容载体，根据type区分内容
     */
    private ElementContent content;
}