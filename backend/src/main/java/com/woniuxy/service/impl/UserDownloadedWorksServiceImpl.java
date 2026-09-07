package com.woniuxy.service.impl;

import com.woniuxy.entity.UserDownloadedWorks;
import com.woniuxy.entity.VO.UserDownloadWorkVO;
import com.woniuxy.entity.Works;
import com.woniuxy.repository.UserDownloadedWorksRepository;
import com.woniuxy.repository.WorksRepository;
import com.woniuxy.service.UserDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDownloadedWorksServiceImpl implements UserDownloadService {

    private final UserDownloadedWorksRepository downloadRepo;
    private final WorksRepository worksRepository;

    @Override
    public List<UserDownloadWorkVO> listMyDownload(Integer userId) {
        return downloadRepo.findMyDownloadVO(userId);
    }

    @Override
    public boolean recordDownload(String taskId, Integer userId) {
        // 1. 通过 taskId 找到对应的 Works 记录
        Optional<Works> worksOpt = worksRepository.findByTaskId(taskId);
        if (worksOpt.isEmpty()) {
            return false;
        }
        Works works = worksOpt.get();
        Long worksId = works.getWorksId();

        // 2. 查是否已有下载记录（upsert 逻辑）
        Optional<UserDownloadedWorks> existing = downloadRepo.findByUserIdAndWorksId(userId, worksId);
        if (existing.isPresent()) {
            // 已有记录，更新下载时间
            UserDownloadedWorks record = existing.get();
            record.setDownloadTime(LocalDateTime.now());
            downloadRepo.save(record);
        } else {
            // 无记录，新增
            UserDownloadedWorks record = new UserDownloadedWorks();
            record.setUserId(userId);
            record.setWorksId(worksId);
            // downloadTime、createTime 由 @PrePersist 自动填充
            downloadRepo.save(record);
        }
        return true;
    }
}