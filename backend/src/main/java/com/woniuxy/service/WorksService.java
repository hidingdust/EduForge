package com.woniuxy.service;
import com.woniuxy.entity.DTO.WorksDTO;
import com.woniuxy.entity.VO.WorksVO;
import org.springframework.data.domain.Page;

public interface WorksService {
    // 增加 loginUserId 参数，可以传null（未登录用户）
    Page<WorksVO> findByKeyWord(WorksDTO worksDTO, Long loginUserId);

    Page<WorksVO> findMyWorks(WorksDTO worksDTO, Long userId, Long loginUserId);

    void deleteWorks(Long worksId,Long loginUserId);

    void likeWorks(Long worksId, Long userId);
    void addViewCount(Long worksId);
    void cancelLikeWorks(Long worksId, Long userId);

    Page<WorksVO> findMyLikedWorks(WorksDTO worksDTO, Long userId);
}