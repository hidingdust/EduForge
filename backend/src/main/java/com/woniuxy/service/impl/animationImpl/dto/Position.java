package com.woniuxy.service.impl.animationImpl.dto;

import lombok.Data;

@Data
public class Position {
    private Double x;
    private Double y;
    /**
     * unit = "px" | "%"
     */
    private String unit;
}