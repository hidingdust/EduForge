package com.woniuxy.service.impl.animationImpl.dto;

import lombok.Data;

@Data
public class ElementContent {
    // type = text 使用
    private String text;

    // type = shape 使用 rectangle circle polygon
    private String shape;
    private String fill;
    private String stroke;

    // type = line 使用
    private Position startPoint;
    private Position endPoint;

    // type = image 使用
    private String imageUrl;

    // 其他扩展内容
    private Object extra;
}