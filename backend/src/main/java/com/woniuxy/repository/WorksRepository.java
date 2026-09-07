package com.woniuxy.repository;

import com.woniuxy.entity.Works;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 核心改动：在后面加上 , JpaSpecificationExecutor<Works>
public interface WorksRepository extends JpaRepository<Works, Long>, JpaSpecificationExecutor<Works> {

    /**
     * 按 taskId 查 Works
     */
    Optional<Works> findByTaskId(String taskId);

    @Query("select w from Works w left join Category c on w.categoryId = c.id " +
            "where w.deleted = false " +
            "and (:isPublic is null or w.isPublic = :isPublic or (w.isPublic is null and :isPublic = true)) " +
            "and (:keyWord is null or w.title like concat(concat('%', :keyWord), '%')) " +
            "and (:categoryId is null or w.categoryId = :categoryId or c.parentId = :categoryId)")
    Page<Works> findWorksByCondition(
            @Param("keyWord") String keyWord,
            @Param("categoryId") Long categoryId,
            @Param("isPublic") Boolean isPublic,
            Pageable pageable
    );

    @Query("select w from Works w left join Category c on w.categoryId = c.id " +
            "where w.deleted = false " +
            "and w.userId = :userId " +
            "and (:keyWord is null or w.title like concat(concat('%', :keyWord), '%')) " +
            "and (:categoryId is null or w.categoryId = :categoryId or c.parentId = :categoryId)")
    Page<Works> findMyWorksByCondition(
            @Param("keyWord") String keyWord,
            @Param("categoryId") Long categoryId,
            @Param("userId") Long userId,
            Pageable pageable
    );
}