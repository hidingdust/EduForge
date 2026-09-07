package com.woniuxy.service;

import com.woniuxy.entity.DTO.EduResourcePublishDTO;
import com.woniuxy.entity.PO.RenderPO;
import com.woniuxy.entity.projection.RenderTaskSimpleProjection;

public interface UploadService {


    String publishEduResource(Integer userId, EduResourcePublishDTO dto);

    // 返回投影对象，不是RenderRepository
    RenderTaskSimpleProjection getTaskByTaskId(String taskId);

    RenderPO getRenderPOByTaskId(String taskId);
}