package com.woniuxy.controller;

import com.woniuxy.constant.Auth;
import com.woniuxy.entity.DTO.EduResourcePublishDTO;
import com.woniuxy.entity.DTO.TaskStartDTO;
import com.woniuxy.entity.PO.RenderPO;
import com.woniuxy.entity.PO.UserPO;
import com.woniuxy.entity.projection.RenderTaskSimpleProjection;
import com.woniuxy.repository.RenderRepository;
import com.woniuxy.repository.UserRepository;
import com.woniuxy.service.DocRenderGraphService;
import com.woniuxy.service.UploadService;
import com.woniuxy.utils.JWTUtils;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;
    private final DocRenderGraphService docRenderGraphService;
    private final RenderRepository renderRepository;
    private final UserRepository userRepository;
    // ====================== 【新增接口：发布教育资源完整版】======================

    /**
     * 发布教育资源：标题+分类+封面+多资源文件，生成课堂任务
     * 前端form-data提交
     */
    @PostMapping("/publish")
    public ResponseResult publishResource(
            @RequestHeader(Auth.AUTHORIZATION) String token,
            @ModelAttribute EduResourcePublishDTO dto) {

        // token 中存的是手机号，需先查出对应的 user_id
        String phone = JWTUtils.getPhoneFromAuthHeader(token);
        UserPO user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String taskId = uploadService.publishEduResource(user.getUserId(), dto);
        return new ResponseResult(200, "资源发布任务已提交", taskId);
    }

    // 必须加 produces = MediaType.TEXT_EVENT_STREAM_VALUE SSE必需
    // 把 @PostMapping 改为 @GetMapping，参数改成 @RequestParam
    @GetMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> taskStart(@RequestParam String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new RuntimeException("taskId不能为空");
        }
        RenderTaskSimpleProjection proj = uploadService.getTaskByTaskId(taskId);
        TaskStartDTO dto = new TaskStartDTO();
        dto.setTaskId(taskId);
        dto.setDocUrl(proj.getDocUrl());
        dto.setUserRequirements(proj.getUserRequirements());
        return docRenderGraphService.startTask(dto);
    }

    /**
     * 根据taskId获取任务最终产出结果
     */
    @GetMapping("/result/{taskId}")
    public ResponseResult getTaskResult(@PathVariable String taskId) {
        RenderPO renderPO = renderRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在,taskId:" + taskId));

        Object resultJson = renderPO.getResultJson();
        // 先用2个参数的构造，只放code、msg，不走带data的构造器
        ResponseResult resp = new ResponseResult(200, "查询成功");
        // putKey强制写入，就算null也强制生成 "data":null
        resp.putKey("data", resultJson);
        return resp;
    }

    /**
     * 查询任务状态 + 断点信息（断点续传专用）
     * 前端在启动任务前调用：resumable=true 且 hasCheckpoint=true 时提示"断点续跑"
     * 返回字段：
     *  - taskId       任务ID
     *  - taskStatus   WAITING / RUNNING / FAIL / SUCCESS
     *  - hasResult    是否已有最终结果
     *  - hasCheckpoint Redis 中是否已有断点（需 Redis 可用，异常时返回 false）
     *  - resumable    任务是否处于"可续跑"状态（FAIL/RUNNING）
     */
    @GetMapping("/status/{taskId}")
    public ResponseResult getTaskStatus(@PathVariable String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new RuntimeException("taskId不能为空");
        }
        RenderPO renderPO = renderRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在,taskId:" + taskId));

        boolean hasResult = renderPO.getResultJson() != null && !renderPO.getResultJson().isBlank();
        boolean resumable = "FAIL".equals(renderPO.getTaskStatus()) || "RUNNING".equals(renderPO.getTaskStatus());

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("taskStatus", renderPO.getTaskStatus());
        data.put("hasResult", hasResult);
        data.put("hasCheckpoint", docRenderGraphService.hasCheckpoint(taskId));
        data.put("resumable", resumable);

        ResponseResult resp = new ResponseResult(200, "查询成功");
        resp.putKey("data", data);
        return resp;
    }
}