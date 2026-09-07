package com.woniuxy.entity.DTO;

import lombok.Data;

import java.util.List;

@Data
public class SupervisorDecisionDTO {
    /**
     * 思考过程
     * 作用：LLM 内部推理日志，记录「为什么选择调度这些 Agent、当前任务完成度判断依据」
     * 使用场景：
     * 调试日志，排查监督者决策异常
     * 可以存入 state 快照，方便后续追溯工作流执行逻辑
     * 不参与路由判断，仅用于记录
     */
    private String thought;
    /**
     * 待执行的目标智能体名称列表【核心路由字段】
     * 作用：告诉工作流接下来要运行哪些子 Agent
     * 约束：
     * 字符串必须和 Graph 中注册的 Agent 节点名称完全一致
     * doc2PPTAgent / doc2AnimationAgent / doc2FlowchartAgent / doc2GameAgent / DocParserAgent
     * 规则（和你提示词保持统一）：
     * 非空数组：执行列表内智能体
     * 空集合 []：代表没有需要执行的 Agent，配合isFinished结束任务
     * 当前你的 Graph 实现：串行模式，只会读取 targetAgents.get(0)；
     * 后续框架支持并行，可一次性下发多个 Agent 并行执行。
     */
    private List<String> targetAgents;
    /**
     * 执行指令 / 任务要求
     * 作用：监督者下发给子 Agent 的任务详细要求
     * 流转方式：
     * Supervisor 输出后，代码存入全局 state 的agent_instruction，所有子 Agent 读取该字段执行任务
     * 价值：
     * 用户原始需求比较宽泛，监督者可以整理、细化指令，统一约束子 Agent 输出风格、内容范围
     */
    private String instruction;
    /**
     * 任务是否全部完成【终止标记】
     * 作用：工作流最大判断条件
     * 判断逻辑（你 Graph 代码）：
     * if (loopCount >=10 || decision.getIsFinished()) → 进入结束节点
     * 取值规范：
     * true：所有产物（PPT / 动画 / 流程图 / 小游戏）全部生成合格，工作流终止
     * false：还有任务需要执行，继续调度子 Agent
     */
    private Boolean isFinished;
    /**示例
     * {
     *   "thought": "用户要求生成PPT和流程图，文档尚未解析，优先执行文档解析Agent",
     *   "targetAgents": ["DocParserAgent"],
     *   "instruction": "解析上传文档，抽取知识点、文本段落，生成标准化文档上下文",
     *   "isFinished": false
     * }
     */
}