package com.woniuxy.repository;

import com.woniuxy.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByParentId(Integer parentId);

    /**
     * 按分类名精确匹配，deleted=false 的可见分类
     */
    List<Category> findByNameAndDeletedFalse(String name);

    /**
     * 按 parentId + name 查二级分类
     */
    List<Category> findByParentIdAndNameAndDeletedFalse(Integer parentId, String name);
}
