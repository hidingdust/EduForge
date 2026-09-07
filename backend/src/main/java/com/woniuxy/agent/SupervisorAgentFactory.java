package com.woniuxy.agent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.woniuxy.entity.DTO.SupervisorDecisionDTO;
import com.woniuxy.service.impl.animationImpl.service.impl.Text2AnimationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupervisorAgentFactory {
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private final DocParserAgentFactory docParserAgentFactory;

    // 注入各个独立子工作流的构建器 子工作流的名字
    private final DocParserSubNodeFactory docParserSubNodeFactory;
    private final Text2AnimationServiceImpl text2AnimationServiceImpl;
    private final Doc2PptSubNodeFactory doc2PptSubNodeFactory;
    private final Doc2GameSubNodeFactory doc2GameSubNodeFactory;
    private final FileGenerateSubNodeFactory fileGenerateSubNodeFactory;

    public StateGraph createGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                "messages", KeyStrategy.APPEND,
                "clean_text", KeyStrategy.REPLACE,
                FileGenerateSubNodeFactory.PPT_JSON, KeyStrategy.REPLACE,
                FileGenerateSubNodeFactory.TASK_ID, KeyStrategy.REPLACE,
                "doc2Animation", KeyStrategy.REPLACE,
                FileGenerateSubNodeFactory.PPT_DOWNLOAD_URL, KeyStrategy.REPLACE
        );
        StateGraph graph = new StateGraph(keyStrategyFactory);
        addAllNodes(graph);
        addAllEdges(graph);
        return graph;
    }

    private void addAllNodes(StateGraph graph) throws GraphStateException {
        //通用占位节点
        graph.addNode("taskStartNode", AsyncNodeAction.node_async(state -> {
            if (state.value("loopCount").isEmpty()) {
                state.updateState(Map.of("loopCount", 0));
            }
            return Map.of();
        }));
        graph.addNode("taskEndNode", AsyncNodeAction.node_async(state -> {
            if (state.value("isForcedComplete").isPresent()) {
                if (state.value("isForcedComplete").get().equals(true)) {
                    log.warn("任务因连续10次未完成被强制终止 | 当前状态: {}", state);
                    state.updateState(Map.of("finalResult", "警告：任务因超时限制强制完成，结果可能不完整"));
                }
            }
            state.updateState(Map.of("loopCount", 0));
            return Map.of();
        }));

        ReactAgent supervisorAgent = docParserAgentFactory.supervisorAgent();
        graph.addNode("supervisorAgent", supervisorAgent.asNode(true, false));

        //注册子工作流节点
        // 节点名称严格对应 Supervisor targetAgents 中的名称
        graph.addNode("docParserWorkflow", docParserSubNodeFactory.createGraph());
        graph.addNode("doc2PPTAgent", doc2PptSubNodeFactory.createGraph());
        graph.addNode("doc2AnimationAgent", AsyncNodeAction.node_async(text2AnimationServiceImpl::text2Animation));
        graph.addNode("doc2GameAgent", doc2GameSubNodeFactory.createGraph());
        // 文件生成子图，监督者满足条件直接调度此节点
        // 子图自动继承顶层全局State，直接读取PPT_JSON、animation_json、taskId，无需手动封装入参
        graph.addNode("fileGenerateAgent", fileGenerateSubNodeFactory.createGraph());
    }

    private void addAllEdges(StateGraph graph) throws GraphStateException {
        graph.addEdge(StateGraph.START, "taskStartNode");
        graph.addEdge("taskStartNode", "supervisorAgent");
        graph.addConditionalEdges("supervisorAgent",
                AsyncEdgeActionWithConfig.edge_async((state, config) -> {
                    AssistantMessage assistantMsg = state.value("supervisor")
                            .map(obj -> (AssistantMessage) obj)
                            .orElseThrow(() -> new RuntimeException("监督者决策数据缺失"));
                    String jsonRaw = assistantMsg.getText();

                    SupervisorDecisionDTO decision;
                    try {
                        decision = objectMapper.readValue(jsonRaw, SupervisorDecisionDTO.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("监督者输出JSON解析失败，原始文本：" + jsonRaw, e);
                    }

                    int loopCount = state.value("loopCount", Integer.class).orElse(0);
                    loopCount++;
                    state.updateState(Map.of("loopCount", loopCount));
                    if (loopCount >= 10) {
                        state.updateState(Map.of("isForcedComplete", true));
                        return "FINISH";
                    }
                    if (decision.getIsFinished()) {
                        return "FINISH";
                    }
                    if (!decision.getTargetAgents().isEmpty()) {
                        return decision.getTargetAgents().get(0);
                    }
                    return "FINISH";
                }), Map.of(
                        "docParserWorkflow", "docParserWorkflow",
                        "doc2PPTAgent", "doc2PPTAgent",
                        "doc2AnimationAgent", "doc2AnimationAgent",
                        "doc2GameAgent", "doc2GameAgent",
                        "fileGenerateAgent", "fileGenerateAgent",
                        "FINISH", "taskEndNode"
                )
        );

        graph.addEdge("docParserWorkflow", "supervisorAgent");
        graph.addEdge("doc2PPTAgent", "supervisorAgent");
        graph.addEdge("doc2AnimationAgent", "supervisorAgent");
        graph.addEdge("doc2GameAgent", "supervisorAgent");
        graph.addEdge("fileGenerateAgent", "taskEndNode");
        graph.addEdge("taskEndNode", StateGraph.END);
    }
}