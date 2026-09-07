package com.woniuxy.service;

import com.woniuxy.entity.DTO.TaskCreateDTO;
import com.woniuxy.entity.DTO.TaskStartDTO;
import com.woniuxy.entity.VO.RenderVO;
import com.woniuxy.service.impl.DocRenderGraphServiceImpl;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface DocRenderGraphService {


    RenderVO createTask(TaskCreateDTO taskCreateDTO);

    Flux<Map<String, Object>> startTask(TaskStartDTO taskStartDTO);

    RenderVO createTaskWithFile(String userRequirements, MultipartFile docFile);

    DocRenderGraphServiceImpl.DocWorkflowResp getTaskResult(String taskId);

    /**
     * 判断 Redis 中是否已存在该任务的断点（checkpoint）。
     * @return true = 已有断点，可续跑；false = 无断点，需从头执行
     */
    boolean hasCheckpoint(String taskId);

}
