package com.woniuxy.service.impl.animationImpl.dto;

import lombok.Data;

@Data
public class Size {
    // 由 Double 修改为 String，支持 "200" / "auto"
    private String width;
    private String height;
    /**
     * unit = "px" | "%"
     */
    private String unit;
}