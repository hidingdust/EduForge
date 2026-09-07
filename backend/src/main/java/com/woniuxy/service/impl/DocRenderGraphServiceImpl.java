package com.woniuxy.service.impl;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.woniuxy.agent.SupervisorAgentFactory;
import com.woniuxy.entity.DTO.TaskCreateDTO;
import com.woniuxy.entity.DTO.TaskStartDTO;
import com.woniuxy.entity.PO.RenderPO;
import com.woniuxy.entity.VO.RenderVO;
import com.woniuxy.repository.RenderRepository;
import com.woniuxy.utils.OssUtil;
import com.woniuxy.utils.TaskStatusJudgement;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocRenderGraphServiceImpl implements com.woniuxy.service.DocRenderGraphService {
    private final RenderRepository renderRepository;
    private final TaskStatusJudgement taskStatusJudgement;
    private final SupervisorAgentFactory supervisorAgentFactory;
    private final RedissonClient redissonClient;
    private final OssUtil ossUtil;

    // ===================== 新增：对外返回DTO，完全对齐你给的JSON格式 =====================
    @Data
    public static class DocWorkflowResp {
        private Integer code;
        private String msg;
        private RenderVO data;


        public static DocWorkflowResp success(RenderVO data) {
            DocWorkflowResp resp = new DocWorkflowResp();
            resp.setCode(200);
            resp.setMsg("success");
            resp.setData(data);
            return resp;
        }

        public static DocWorkflowResp fail(int code, String msg) {
            DocWorkflowResp resp = new DocWorkflowResp();
            resp.setCode(code);
            resp.setMsg(msg);
            return resp;
        }
    }

    /**
     * 清洗Supervisor返回JSON，剥离markdown标记，提取{}片段
     */
    private String cleanSupervisorJson(String rawResp) {
        if (rawResp == null) return "{}";
        String json = rawResp.replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
        int start = json.indexOf("{");
        int end = json.lastIndexOf("}");
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        return json;
    }

    @Override
    public RenderVO createTask(TaskCreateDTO taskCreateDTO) {
        RenderPO renderPO = dto2Po(taskCreateDTO);
        RenderPO res = renderRepository.save(renderPO);
        return po2Vo(res);
    }

    @Override
    public Flux<Map<String, Object>> startTask(TaskStartDTO taskStartDTO) {
        // ===== 断点续传：允许 WAITING 首次启动、FAIL/RUNNING 续跑，SUCCESS 拒绝 =====
        TaskStatusJudgement.StartJudge judge = taskStatusJudgement.judgeTaskStart(taskStartDTO.getTaskId());
        if (!judge.isAllow()) {
            Map<String, Object> notice = new HashMap<>();
            notice.put("msg", judge.getReason());
            notice.put("type", "notice");
            notice.put("taskId", taskStartDTO.getTaskId()); // ✅加上taskI
            return Flux.just(notice);
        }

        taskStatusJudgement.updateTaskRunning(taskStartDTO.getTaskId());

        try {
            StateGraph workflow = supervisorAgentFactory.createGraph();
            // ===== 取消注释：Redis 断点续传 Saver（checkpoint 持久化到 Redis） =====
            // 相同 threadId 再次 stream 时，框架会自动从上次保存的 checkpoint 恢复执行
            RedisSaver redisSaver = newRedisSaver();
            CompileConfig compileConfig = CompileConfig.builder()
                    .saverConfig(SaverConfig.builder()
                            .register(redisSaver)
                            .build())
                    // releaseThread 保持默认 false：任务失败/中断后 checkpoint 不释放，供续跑；
                    // 任务成功时在 doFinally 中手动释放（见 releaseCheckpoint）
                    .build();
            CompiledGraph graph = workflow.compile(compileConfig);

            String taskId = taskStartDTO.getTaskId();
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(taskId)
                    .addMetadata("userId", 1)
                    .build();

            // ===== 断点检测：Redis 中已有该 threadId 的 checkpoint → 续跑 =====
            boolean resumable = graph.stateOf(runnableConfig).isPresent();
            log.info("任务[{}] {}执行", taskId, resumable ? "检测到断点，将从上次位置续跑" : "首次启动，");

            Optional<RenderPO> opt = renderRepository.findByTaskId(taskId);
            if (opt.isEmpty()) {
                throw new RuntimeException("找不到该任务：" + taskId);
            }
            RenderPO renderPO = opt.get();

            Map<String, Object> initState = new HashMap<>();
            initState.put("taskId", taskId);
            // 续跑时无需重复注入文档/需求等初始状态：checkpoint 中已保存完整全局状态，
            // 仅传 taskId 即可，框架会以 checkpoint 状态为准继续执行（幂等安全）；
            // 首次启动才需要注入 docUrl / userRequirements / loopCount / messages
            if (!resumable) {
                initState.put("docUrl", renderPO.getDocUrl());
                initState.put("userRequirements", taskStartDTO.getUserRequirements());
                initState.put("loopCount", 0);
                initState.put("messages", new java.util.ArrayList<>());
            }

            Flux<NodeOutput> stream = graph.stream(initState, runnableConfig);

            AtomicReference<Map<String, Object>> finalStateRef = new AtomicReference<>();

            Flux<Map<String, Object>> eventFlux = stream
                    .doOnNext(output -> {
                        if (output.state() != null) {
                            finalStateRef.set(output.state().data());
                        }
                        String nodeName = output.node();
                        if ("supervisorAgent".equals(nodeName) && output.state() != null) {
                            Map<String, Object> fullState = output.state().data();
                            Object supervisorOutput = fullState.get("supervisorPlan");
                            if (supervisorOutput != null) {
                                String raw = String.valueOf(supervisorOutput);
                                String cleanJson = cleanSupervisorJson(raw);
                                log.info("==== Supervisor清洗后调度JSON ====\n{}", cleanJson);
                            }
                        }
                    })
                    .map(output -> {
                        String nodeName = output.node();
                        String agentType = switch (nodeName) {
                            case "docParserWorkflow" -> "parser";
                            case "doc2PPTAgent" -> "ppt";
                            case "doc2AnimationAgent" -> "animation";
                            case "doc2GameAgent" -> "game";
                            case "taskStartNode", "taskEndNode", "supervisorAgent", "fileGenerateAgent" -> "system";
                            default -> "other";
                        };
                        Map<String, Object> map = new HashMap<>();
                        map.put("type", "agent_finish");
                        map.put("node", nodeName);
                        map.put("agentType", agentType);
                        map.put("msg", nodeName + " 执行完成，可以开始渲染");
                        map.put("taskId", taskId); // ✅每个agent_finish事件带上taskId
                        return map;
                    })
                    // ============ 追加：流正常结束时，下发task_finish事件 ============
                    .concatWith(Flux.just(Map.of(
                            "type", "task_finish",
                            "msg", "全部工作流执行完成，请获取结果",
                            "taskId", taskId
                    )))
                    .doFinally(signalType -> {
                        log.info("任务流终止，signalType={}, taskId={}", signalType, taskId);
                        Map<String, Object> finalState = finalStateRef.get();
                        String json = null;
                        if (finalState != null && !finalState.isEmpty()) {
                            // ✅ 只提取业务结果字段，过滤掉messages/supervisor/loopCount等流程内部状态
                            Map<String, Object> outputResult = new HashMap<>();
                            outputResult.put("clean_text", finalState.get("clean_text"));
                            outputResult.put("PPT_JSON", finalState.get("PPT_JSON"));
                            outputResult.put("doc2Game", finalState.get("doc2Game"));
                            outputResult.put("animation_json", finalState.get("animation_json"));
                            outputResult.put("pptDownloadUrl", finalState.get("pptDownloadUrl"));
                            outputResult.put("questions", finalState.get("questions"));
                            json = JSON.toJSONString(outputResult, JSONWriter.Feature.WriteNonStringKeyAsString);
                        }
                        try {
                            switch (signalType) {
                                case ON_COMPLETE:
                                    log.info("任务正常执行完毕 taskId={}", taskId);
                                    if (json != null) {
                                        // 任务成功：释放 Redis checkpoint，避免堆积（同 threadId 将不再可续跑）
                                        releaseCheckpoint(redisSaver, taskId);
                                        taskStatusJudgement.updateTaskSuccess(taskId, json);
                                    } else {
                                        taskStatusJudgement.updateTaskFail(taskId, "最终状态为空");
                                    }
                                    break;
                                case ON_ERROR:
                                    log.error("任务执行异常 taskId={}", taskId);
                                    taskStatusJudgement.updateTaskFail(taskId, json != null ? json : "工作流执行异常");
                                    break;
                                case CANCEL:
                                    log.warn("客户端主动断开SSE连接 taskId={}", taskId);
                                    taskStatusJudgement.updateTaskFail(taskId, json != null ? json : "客户端提前断开");
                                    break;
                                default:
                                    break;
                            }
                        } catch (Exception e) {
                            log.error("任务收尾更新数据库异常 taskId={}", taskId, e);
                        }
                    })
                    .onErrorResume(e -> {
                        log.error("工作流执行异常 taskId={}", taskId, e);
                        Map<String, Object> errorEvent = new HashMap<>();
                        errorEvent.put("type", "task_error");
                        errorEvent.put("msg", "任务执行失败：" + e.getMessage());
                        errorEvent.put("taskId", taskId); // ✅错误事件也带上taskId
                        return Flux.just(errorEvent);
                    });

            return eventFlux;
        } catch (Exception e) {
            log.error("构建工作流同步异常", e);
            taskStatusJudgement.updateTaskFail(taskStartDTO.getTaskId(), e.getMessage());
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("type", "task_error");
            errorMap.put("msg", "任务初始化失败：" + e.getMessage());
            return Flux.just(errorMap);
        }
    }

    @Override
    public RenderVO createTaskWithFile(String userRequirements, MultipartFile docFile) {
        if (docFile.isEmpty()) {
            throw new RuntimeException("请上传文档");
        }
        String originalFilename = docFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new RuntimeException("文件名称不能为空");
        }
        if (!originalFilename.endsWith(".doc") && !originalFilename.endsWith(".docx")) {
            throw new RuntimeException("仅支持 doc / docx 文档");
        }

        try {
            String ossUrl = ossUtil.uploadMultipartFile(docFile, "ai_work_doc");
            TaskCreateDTO dto = new TaskCreateDTO();
            dto.setUserRequirements(userRequirements);
            dto.setDocUrl(ossUrl);
            return createTask(dto);
        } catch (IOException e) {
            log.error("文档上传OSS失败", e);
            throw new RuntimeException("文件上传OSS异常：" + e.getMessage());
        }
    }

    /**
     * 查询任务结果，返回你指定完整外层格式
     */
    @Override
    public DocWorkflowResp getTaskResult(String taskId) {
        RenderPO po = renderRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        RenderVO renderVO = po2Vo(po);
        return DocWorkflowResp.success(renderVO);
    }

    // ============== 断点续传：checkpoint 查询与释放 ==============

    /**
     * 判断 Redis 中是否已存在该任务的断点（checkpoint）。
     * 供前端在启动前查询，决定是否提示"断点续跑"。
     */
    @Override
    public boolean hasCheckpoint(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return false;
        }
        try {
            StateGraph workflow = supervisorAgentFactory.createGraph();
            CompileConfig compileConfig = CompileConfig.builder()
                    .saverConfig(SaverConfig.builder()
                            .register(newRedisSaver())
                            .build())
                    .build();
            CompiledGraph graph = workflow.compile(compileConfig);
            return graph.stateOf(RunnableConfig.builder().threadId(taskId).build()).isPresent();
        } catch (Exception e) {
            log.warn("查询任务[{}] checkpoint 异常，按无断点处理", taskId, e);
            return false;
        }
    }

    /**
     * 构建 Redis 断点 Saver（每次新建，避免共享 RedissonClient 连接状态）
     */
    private RedisSaver newRedisSaver() {
        return RedisSaver.builder()
                .redisson(redissonClient)
                .build();
    }

    /**
     * 任务成功后释放 Redis checkpoint：
     * 标记该 threadId 已释放，此后 stateOf 返回 empty → 无法续跑，
     * 避免同 threadId 任务重复恢复执行，也避免 Redis 中 checkpoint 无限堆积。
     * 任务失败/中断时不释放，保留断点供续跑。
     */
    private void releaseCheckpoint(RedisSaver saver, String taskId) {
        try {
            saver.release(RunnableConfig.builder().threadId(taskId).build());
            log.info("任务[{}] 成功，已释放 Redis checkpoint", taskId);
        } catch (Exception e) {
            log.warn("任务[{}] 释放 Redis checkpoint 失败（不影响结果落库）", taskId, e);
        }
    }

    private RenderVO po2Vo(RenderPO res) {
        RenderVO renderVO = new RenderVO();
        renderVO.setTaskId(res.getTaskId());
        renderVO.setDocUrl(res.getDocUrl());
        renderVO.setTaskStatus(res.getTaskStatus());
        renderVO.setResultJson(res.getResultJson());
        renderVO.setCreateTime(res.getCreateTime());
        renderVO.setUpdateTime(res.getUpdateTime());
        return renderVO;
    }

    private RenderPO dto2Po(TaskCreateDTO taskCreateDTO) {
        RenderPO renderPO = new RenderPO();
        renderPO.setTaskId(UUID.randomUUID().toString());
        renderPO.setDocUrl(taskCreateDTO.getDocUrl());
        renderPO.setUserRequirements(taskCreateDTO.getUserRequirements());
        renderPO.setTaskStatus("WAITING");
        return renderPO;
    }
}