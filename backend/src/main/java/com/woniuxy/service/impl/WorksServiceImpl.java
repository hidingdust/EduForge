package com.woniuxy.service.impl;

import com.woniuxy.entity.DTO.WorksDTO;
import com.woniuxy.entity.VO.WorksVO;
import com.woniuxy.entity.WorkLike;
import com.woniuxy.entity.Works;
import com.woniuxy.repository.WorkLikeRepository;
import com.woniuxy.repository.WorksRepository;
import com.woniuxy.service.WorksService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorksServiceImpl implements WorksService {

    private final WorksRepository worksRepository;
    private final WorkLikeRepository workLikeRepository;

    @Override
    public Page<WorksVO> findByKeyWord(WorksDTO worksDTO, Long loginUserId) {
        String keyWord = worksDTO.getKeyWord();
        if (keyWord != null && keyWord.trim().isEmpty()) {
            worksDTO.setKeyWord(null);
        }

        Sort sort = getSort(worksDTO.getSortType());
        int pageNum = worksDTO.getPageNum() == null ? 1 : worksDTO.getPageNum();
        int pageSize = worksDTO.getPageSize() == null ? 10 : worksDTO.getPageSize();
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        Page<Works> page = worksRepository.findWorksByCondition(
                worksDTO.getKeyWord(),
                worksDTO.getCategoryId(),
                worksDTO.getIsPublic(),
                pageable
        );
        // 转换并填充isLiked
        return convertPageWithLikeFlag(page, loginUserId);
    }

    @Override
    public Page<WorksVO> findMyWorks(WorksDTO worksDTO, Long userId, Long loginUserId) {
        String keyWord = worksDTO.getKeyWord();
        if (keyWord != null && keyWord.trim().isEmpty()) {
            worksDTO.setKeyWord(null);
        }

        Sort sort = getSort(worksDTO.getSortType());
        int pageNum = worksDTO.getPageNum() == null ? 1 : worksDTO.getPageNum();
        int pageSize = worksDTO.getPageSize() == null ? 10 : worksDTO.getPageSize();
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        Page<Works> worksPage = worksRepository.findMyWorksByCondition(
                worksDTO.getKeyWord(),
                worksDTO.getCategoryId(),
                userId,
                pageable
        );
        return convertPageWithLikeFlag(worksPage, loginUserId);
    }

    @Override
    @Transactional
    public void deleteWorks(Long worksId, Long loginUserId) {
        Optional<Works> worksOpt = worksRepository.findById(worksId);
        if (worksOpt.isEmpty()) {
            throw new RuntimeException("作品不存在");
        }
        Works works = worksOpt.get();

        if (!works.getUserId().equals(loginUserId)) {
            throw new RuntimeException("无权限删除他人作品");
        }
        works.setDeleted(true);
        worksRepository.save(works);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeWorks(Long worksId, Long userId) {
        Optional<WorkLike> likeOpt = workLikeRepository.findByWorksIdAndUserId(worksId, userId);
        if (likeOpt.isPresent()) {
            throw new RuntimeException("您已经点赞过该作品，不能重复点赞");
        }
        Works works = worksRepository.findById(worksId)
                .orElseThrow(() -> new RuntimeException("作品不存在"));

        WorkLike workLike = new WorkLike();
        workLike.setWorksId(worksId);
        workLike.setUserId(userId);
        workLikeRepository.save(workLike);

        works.setLikeCount(works.getLikeCount() == null ? 1 : works.getLikeCount() + 1);
        worksRepository.save(works);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addViewCount(Long worksId) {
        Works works = worksRepository.findById(worksId)
                .orElseThrow(() -> new RuntimeException("作品不存在"));
        works.setViewCount(works.getViewCount() == null ? 1 : works.getViewCount() + 1);
        worksRepository.save(works);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLikeWorks(Long worksId, Long userId) {
        Optional<WorkLike> likeOpt = workLikeRepository.findByWorksIdAndUserId(worksId, userId);
        if (likeOpt.isEmpty()) {
            throw new RuntimeException("尚未点赞，无法取消");
        }
        workLikeRepository.deleteByWorksIdAndUserId(worksId, userId);

        Works works = worksRepository.findById(worksId)
                .orElseThrow(() -> new RuntimeException("作品不存在"));
        if (works.getLikeCount() != null && works.getLikeCount() > 0) {
            works.setLikeCount(works.getLikeCount() - 1);
            worksRepository.save(works);
        }
    }

    @Override
    public Page<WorksVO> findMyLikedWorks(WorksDTO worksDTO, Long loginUserId) {
//        return null;
        int pageNum = worksDTO.getPageNum() - 1;
        int pageSize = worksDTO.getPageSize();
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        // 分页查询该用户点赞的作品ID集合
        Page<Long> likedWorksIdPage = workLikeRepository.findLikedWorksIdByUserId(loginUserId, pageable);
        List<Long> worksIdList = likedWorksIdPage.getContent();

        // 当前页没有点赞数据，直接返回空分页
        if (worksIdList.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        // 构造动态条件：worksId在点赞列表 + 关键词、分类过滤 + 排除已删除作品
        Specification<Works> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("worksId").in(worksIdList));
            predicates.add(cb.equal(root.get("deleted"), false));

            if (StringUtils.hasText(worksDTO.getKeyWord())) {
                predicates.add(cb.like(root.get("title"), "%" + worksDTO.getKeyWord() + "%"));
            }
            if (worksDTO.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), worksDTO.getCategoryId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Works> worksList = worksRepository.findAll(spec);

        // 修复：convertToVO → po2Vo
        List<WorksVO> voList = worksList.stream()
                .map(works -> {
                    WorksVO vo = po2Vo(works);
                    vo.setLiked(true);
                    return vo;
                })
                .collect(Collectors.toList());

        // 分页对象：数据、分页参数、总点赞条数
        return new PageImpl<>(voList, pageable, likedWorksIdPage.getTotalElements());
    }

    private Sort getSort(String sortType) {
        if ("viewCount".equals(sortType)) {
            return Sort.by(Sort.Direction.DESC, "viewCount");
        } else if ("likeCount".equals(sortType)) {
            return Sort.by(Sort.Direction.DESC, "likeCount");
        }
        return Sort.by(Sort.Direction.DESC, "createTime");
    }

    /**
     * 分页转换 + 填充isLiked
     */
    private Page<WorksVO> convertPageWithLikeFlag(Page<Works> pageWorks, Long loginUserId) {
        List<WorksVO> voList = pageWorks.getContent().stream()
                .map(works -> {
                    WorksVO vo = po2Vo(works);
                    // 未登录直接false
                    if (loginUserId == null) {
                        vo.setLiked(false);
                        return vo;
                    }
                    // 查询当前用户是否点赞
                    Optional<WorkLike> likeOpt = workLikeRepository.findByWorksIdAndUserId(works.getWorksId(), loginUserId);
                    vo.setLiked(likeOpt.isPresent());
                    return vo;
                }).collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                voList,
                pageWorks.getPageable(),
                pageWorks.getTotalElements()
        );
    }

    private WorksVO po2Vo(Works works) {
        WorksVO vo = new WorksVO();
        vo.setId(works.getWorksId());
        vo.setTaskId(works.getTaskId());
        vo.setCategoryId(works.getCategoryId());
        vo.setTitle(works.getTitle());
        vo.setCoverUrl(works.getCoverUrl());
        vo.setDocUrl(works.getDocUrl());
        vo.setLikeCount(works.getLikeCount() == null ? 0 : works.getLikeCount());
        vo.setViewCount(works.getViewCount() == null ? 0 : works.getViewCount());
        vo.setCommentCount(works.getCommentCount() == null ? 0 : works.getCommentCount());
        vo.setDownloadCount(works.getDownloadCount() == null ? 0 : works.getDownloadCount());
        return vo;
    }
}