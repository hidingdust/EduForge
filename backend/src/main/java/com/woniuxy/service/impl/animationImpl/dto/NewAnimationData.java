package com.woniuxy.service.impl.animationImpl.dto;

import lombok.Data;

import java.util.List;

@Data
public class NewAnimationData {
    /**
     * 结构版本，固定 "1.0"，用于Schema校验
     */
    private String version;

    /**
     * 元信息：标题、描述、总时长等
     */
    private AnimationMetadata metadata;

    /**
     * 全局画布配置
     */
    private AnimationGlobalConfig globalConfig;

    /**
     * 所有画布元素【核心】
     * 不再区分sequence，全部元素平铺在此
     */
    private List<CanvasElement> elements;
}