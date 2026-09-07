package com.woniuxy.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Integer id;

    private String name;

    private Integer parentId;

    private String state;

    // 补上逻辑删除，数据库表category必须要有deleted tinyint(1)
    private Boolean deleted;

    // JPA使用 @Transient 标记非数据库字段，不要用 @TableField
    @Transient
    private List<Category> children;
}