package com.woniuxy.repository;

import com.woniuxy.entity.WorkLike;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface WorkLikeRepository extends JpaRepository<WorkLike, Long> {
    Optional<WorkLike> findByWorksIdAndUserId(Long worksId, Long userId);

    // 新增：取消点赞，删除点赞记录
    void deleteByWorksIdAndUserId(Long worksId, Long userId);
    @Query("SELECT wl.worksId FROM WorkLike wl WHERE wl.userId = :userId ORDER BY wl.createTime DESC")
    Page<Long> findLikedWorksIdByUserId(Long userId, Pageable pageable);
}