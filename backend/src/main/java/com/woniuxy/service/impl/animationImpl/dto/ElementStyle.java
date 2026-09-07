package com.woniuxy.service.impl.animationImpl.dto;

import lombok.Data;

@Data
public class ElementStyle {
    /**
     * 基础透明度 0~1
     */
    private Double opacity;
    /**
     * 初始静态旋转角度
     */
    private Double rotation;
    private String backgroundColor;
    private String borderColor;
    private Double borderWidth;
    /**
     * 文字排版：字号（px，可传字符串如 "28"）
     */
    private String fontSize;
    /**
     * 文字颜色
     */
    private String color;
    /**
     * 文字字重：normal / bold
     */
    private String fontWeight;
    /**
     * 文字对齐：left / center / right
     */
    private String textAlign;
    /**
     * 预留自定义样式
     */
    private Object customStyles;
}