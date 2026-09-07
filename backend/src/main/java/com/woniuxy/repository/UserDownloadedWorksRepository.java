package com.woniuxy.repository;

import com.woniuxy.entity.UserDownloadedWorks;
import com.woniuxy.entity.VO.UserDownloadWorkVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDownloadedWorksRepository extends JpaRepository<UserDownloadedWorks, Long> {

    @Query("""
        SELECT new com.woniuxy.entity.VO.UserDownloadWorkVO(
            w.worksId,
            w.title,
            w.coverUrl,
            COALESCE(CONCAT(pc.name, '/', c.name), c.name),
            udw.downloadTime,
            w.taskId
        )
        FROM UserDownloadedWorks udw
        JOIN Works w ON udw.worksId = w.worksId
        LEFT JOIN Category c ON w.categoryId = c.id
        LEFT JOIN Category pc ON c.parentId = pc.id
        WHERE udw.userId = :userId
          AND udw.deleted = false
          AND w.deleted = false
          AND (c IS NULL OR c.deleted = false)
        ORDER BY udw.downloadTime DESC
        """)
    List<UserDownloadWorkVO> findMyDownloadVO(@Param("userId") Integer userId);

    /**
     * 按 userId + worksId 查找下载记录（用于 upsert：已存在则更新时间，不存在则新增）
     */
    Optional<UserDownloadedWorks> findByUserIdAndWorksId(Integer userId, Long worksId);
}