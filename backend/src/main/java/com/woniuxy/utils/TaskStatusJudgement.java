package com.woniuxy.utils;

import com.woniuxy.entity.PO.RenderPO;
import com.woniuxy.entity.Works;
import com.woniuxy.repository.RenderRepository;
import com.woniuxy.repository.WorksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStatusJudgement {
    private final RenderRepository renderRepository;
    private final WorksRepository worksRepository;

    // Works 状态常量（与 UploadServiceImpl 对齐）
    private static final int WORKS_STATUS_RUNNING = 0;
    private static final int WORKS_STATUS_SUCCESS = 1;
    private static final int WORKS_STATUS_FAIL = 2;

    // 1. 标记任务失败
    @Transactional
    public void updateTaskFail(String taskId, String errorMsg) {
        Optional<RenderPO> opt = renderRepository.findByTaskId(taskId);
        if (opt.isEmpty()) {
            log.warn("updateTaskFail：未找到任务 taskId={}", taskId);
            return;
        }
        RenderPO po = opt.get();
        po.setTaskStatus("FAIL");
        // 错误信息存入stateSnapshot（TEXT字段存放异常信息）
        String safeErrMsg = errorMsg;
        if (safeErrMsg != null && safeErrMsg.length() > 1500) {
            safeErrMsg = safeErrMsg.substring(0, 1500);
        }
        po.setStateSnapshot(safeErrMsg);
        // updateTime由@UpdateTimestamp自动填充，无需手动设置
        renderRepository.save(po);
        // 同步更新 Works 状态
        updateWorksStatus(taskId, WORKS_STATUS_FAIL);
        log.error("任务[{}]状态更新为FAIL, error={}", taskId, safeErrMsg);
    }

    // 2. 任务启动：WAIT → RUNNING
    @Transactional
    public void updateTaskRunning(String taskId) {
        Optional<RenderPO> opt = renderRepository.findByTaskId(taskId);
        if (opt.isEmpty()) {
            log.warn("updateTaskRunning：未找到任务 taskId={}", taskId);
            return;
        }
        RenderPO po = opt.get();
        po.setTaskStatus("RUNNING");
        renderRepository.save(po);
        // 同步更新 Works 状态
        updateWorksStatus(taskId, WORKS_STATUS_RUNNING);
        log.info("任务[{}]状态更新为RUNNING", taskId);
    }

    // 3. 任务正常完成：标记SUCCESS，保存产物resultJson到agent_task
    //    大JSON数据只存agent_task，Works表仅同步status状态位
    @Transactional
    public void updateTaskSuccess(String taskId, String resultJson) {
        Optional<RenderPO> opt = renderRepository.findByTaskId(taskId);
        if (opt.isEmpty()) {
            log.warn("updateTaskSuccess：未找到任务 taskId={}", taskId);
            return;
        }
        RenderPO po = opt.get();
        po.setTaskStatus("SUCCESS");
        po.setResultJson(resultJson);
        renderRepository.save(po);
        // Works 表只同步状态位，不存大 JSON
        updateWorksStatus(taskId, WORKS_STATUS_SUCCESS);
        log.info("任务[{}]执行成功", taskId);
    }

    // 4. 判断任务是否允许启动
    // 返回true：任务已经开始（RUNNING/SUCCESS/FAIL），禁止重复启动
    // 返回false：WAIT状态，可以启动
    public boolean judgeTaskStatus(String taskId) {
        Optional<RenderPO> opt = renderRepository.findByTaskId(taskId);
        // 不存在任务 || 状态不是WAIT → 不能启动
        if (opt.isEmpty()) {
            return true;
        }
        RenderPO po = opt.get();
        return !"WAITING".equals(po.getTaskStatus());
    }

    // ============== 断点续传：任务启动判定（支持 FAIL/RUNNING 续跑） ==============

    /**
     * 启动判定结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class StartJudge {
        /** 是否允许启动/续跑 */
        private final boolean allow;
        /** 判定说明（返回给前端展示） */
        private final String reason;
    }

    /**
     * 断点续传场景下的任务启动判定：
     *  - WAITING          → 首次启动
     *  - FAIL / RUNNING   → 允许重新启动（Redis 中有断点则续跑，无断点则从头重跑）
     *  - SUCCESS          → 拒绝（任务已完成）
     */
    public StartJudge judgeTaskStart(String taskId) {
        Optional<RenderPO> opt = renderRepository.findByTaskId(taskId);
        if (opt.isEmpty()) {
            return new StartJudge(false, "任务不存在，无法启动");
        }
        String status = opt.get().getTaskStatus();
        if ("SUCCESS".equals(status)) {
            return new StartJudge(false, "任务已完成，请勿重复启动");
        }
        if ("WAITING".equals(status)) {
            return new StartJudge(true, "首次启动任务");
        }
        // FAIL / RUNNING → 允许重入：有 checkpoint 则断点续跑，无则重新执行
        return new StartJudge(true, "检测到未完成任务，尝试断点续跑");
    }

    // ============== Works 状态同步（仅 status 字段） ==============

    private void updateWorksStatus(String taskId, int status) {
        try {
            Optional<Works> opt = worksRepository.findByTaskId(taskId);
            if (opt.isPresent()) {
                Works works = opt.get();
                works.setStatus(status);
                worksRepository.save(works);
            } else {
                log.debug("Works 未按 taskId 命中：{}", taskId);
            }
        } catch (Exception e) {
            log.error("更新 Works 状态异常 taskId={}, status={}", taskId, status, e);
        }
    }
}
