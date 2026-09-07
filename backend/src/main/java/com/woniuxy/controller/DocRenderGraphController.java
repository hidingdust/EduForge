package com.woniuxy.controller;

import com.woniuxy.entity.DTO.TaskCreateDTO;
import com.woniuxy.entity.DTO.TaskStartDTO;
import com.woniuxy.entity.VO.RenderVO;
import com.woniuxy.service.DocRenderGraphService;
import com.woniuxy.service.impl.DocRenderGraphServiceImpl;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/render")
@RequiredArgsConstructor
public class DocRenderGraphController {
    private final DocRenderGraphService docRenderGraphService;

    // 原有接口【保留，用于apifox测试传本地路径】
    @PostMapping("/create")
    public ResponseResult taskCreate(@RequestBody TaskCreateDTO taskCreateDTO){
        RenderVO renderVO=docRenderGraphService.createTask(taskCreateDTO);
        return new ResponseResult(renderVO);
    }

    // 原有流式接口【后续建议改成GET + produces SSE】
    @PostMapping("/start")
    public Flux<Map<String, Object>> taskStart(@RequestBody TaskStartDTO taskStartDTO){
        return docRenderGraphService.startTask(taskStartDTO);
    }

    // ====================== 新增：前端网页使用【上传文档+创建任务】======================
    @PostMapping(value = "/uploadCreate")
    public ResponseResult uploadCreateTask(
            @RequestParam("userRequirements") String userRequirements,
            @RequestParam("docFile") MultipartFile docFile
    ) {
        // 交给service：接收文件 → 保存到临时目录 → 组装TaskCreateDTO，创建任务
        RenderVO renderVO = docRenderGraphService.createTaskWithFile(userRequirements, docFile);
        return new ResponseResult(renderVO);
    }

    @GetMapping("/workDetail/{taskId}")
    public DocRenderGraphServiceImpl.DocWorkflowResp getResult(@PathVariable String taskId){
        return docRenderGraphService.getTaskResult(taskId);
    }
}