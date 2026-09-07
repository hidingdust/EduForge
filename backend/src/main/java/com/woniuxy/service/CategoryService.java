package com.woniuxy.service;

import com.woniuxy.entity.Category;
import com.woniuxy.entity.VO.CategoryTreeVO;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> getRootList();

    List<Category> findAll();

    List<Category> getChildByParentId(Integer parentId);

    /**
     * 按分类名查（多结果取第一条）
     */
    Optional<Category> findFirstByName(String name);

    /**
     * 按父级 ID + 子级名查子分类
     */
    Optional<Category> findChildByParentIdAndName(Integer parentId, String name);

    /**
     * 构建两级分类树（仅包含未删除的分类）
     */
    List<CategoryTreeVO> buildCategoryTree();
}
