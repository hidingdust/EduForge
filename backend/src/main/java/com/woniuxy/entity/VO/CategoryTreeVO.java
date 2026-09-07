package com.woniuxy.entity.VO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryTreeVO {
    private Integer id;
    private String name;
    private List<CategoryTreeVO> children = new ArrayList<>();
}
