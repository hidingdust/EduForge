package com.woniuxy.service;

import com.woniuxy.entity.VO.UserDownloadWorkVO;

import java.util.List;

public interface UserDownloadService {
    List<UserDownloadWorkVO> listMyDownload(Integer userId);

    /**
     * 记录用户下载行为：通过 taskId 定位 works，再 upsert 下载记录
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return true=记录成功, false=works不存在
     */
    boolean recordDownload(String taskId, Integer userId);
}
