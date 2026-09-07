package com.woniuxy.entity.DTO;
import lombok.Data;

@Data
public class WorksDTO {
    // 搜索关键词
    private String keyWord;
    // 分类id
    private Long categoryId;
    // 是否公开
    private Boolean isPublic;
    // 页码
    private Integer pageNum;
    // 每页条数
    private Integer pageSize;
    // 排序类型
    private String sortType;
}