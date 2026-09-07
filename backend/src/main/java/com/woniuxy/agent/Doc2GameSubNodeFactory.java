package com.woniuxy.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woniuxy.entity.GameData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Doc2GameSubNodeFactory {

    private final DocParserAgentFactory docParserAgentFactory;
    private final ObjectMapper objectMapper;

    /** 工作流起始节点标识 */
    private static final String NODE_START = "startNode";
    /** 结果解析节点标识 */
    private static final String NODE_PARSE = "parseNode";
    /** 工作流结束节点标识 */
    private static final String NODE_END = "endNode";
    /** 上下文存储原文文本的Key */
    private static final String KEY_CLEAN_TEXT = "clean_text";
    /** 上下文存储最终题目列表的Key */
    private static final String KEY_QUESTIONS = "questions";


    public StateGraph createGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                KEY_QUESTIONS, KeyStrategy.REPLACE
        );
        StateGraph graph = new StateGraph(keyStrategyFactory);
        addAllNodes(graph);
        addAllEdges(graph);
        return graph;
    }

    private void addAllNodes(StateGraph graph) throws GraphStateException {
        graph.addNode(NODE_START, startNode());
        graph.addNode(NODE_PARSE, parseNode());
        graph.addNode(NODE_END, endNode());
    }

    private void addAllEdges(StateGraph graph) throws GraphStateException {
        graph.addEdge(StateGraph.START, NODE_START);
        graph.addEdge(NODE_START, NODE_PARSE);
        graph.addEdge(NODE_PARSE, NODE_END);
        graph.addEdge(NODE_END, StateGraph.END);
    }

    public AsyncNodeAction startNode() {
        return AsyncNodeAction.node_async(state -> {
            String cleanText = (String) state.value(KEY_CLEAN_TEXT)
                    .orElseThrow(() -> new RuntimeException("缺少 clean_text"));
            log.info("📝 开始生成题目，文档长度: {}", cleanText.length());
            return Map.of(KEY_CLEAN_TEXT, cleanText);
        });
    }

    public AsyncNodeAction parseNode() {
        return AsyncNodeAction.node_async(state -> {
            String cleanText = (String) state.value(KEY_CLEAN_TEXT)
                    .orElseThrow(() -> new RuntimeException("缺少 clean_text"));

            try {
                // ✅ 直接在这里调用 Agent，不使用 asNode()
                ReactAgent gameAgent = docParserAgentFactory.doc2GameAgent();
                var subStateOpt = gameAgent.invoke(cleanText);

                if (subStateOpt.isEmpty()) {
                    log.error("Agent 执行返回空");
                    return Map.of(KEY_QUESTIONS, Collections.emptyList());
                }

                var subState = subStateOpt.get();
                Object obj = subState.value("doc2Game")
                        .orElseThrow(() -> new RuntimeException("缺少 doc2Game 输出"));

                List<Map<String, Object>> questionList;

                if (obj instanceof GameData) {
                    GameData gameData = (GameData) obj;
                    questionList = new ArrayList<>();
                    for (GameData.Question q : gameData.getQuestions()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("question", q.getQuestion());
                        map.put("A", q.getA());
                        map.put("B", q.getB());
                        map.put("C", q.getC());
                        map.put("D", q.getD());
                        map.put("answer", q.getAnswer());
                        questionList.add(map);
                    }
                } else if (obj instanceof AssistantMessage) {
                    String gameJson = ((AssistantMessage) obj).getText();
                    questionList = parseJsonQuestion(gameJson);
                } else {
                    questionList = Collections.emptyList();
                }

                log.info("✅ 生成题目: {} 道", questionList.size());
                System.out.println("生成的题目："+questionList);
                return Map.of(KEY_QUESTIONS, questionList);
            } catch (Exception e) {
                log.error("生成题目失败", e);
                return Map.of(KEY_QUESTIONS, Collections.emptyList());
            }
        });
    }

    public AsyncNodeAction endNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("✅ 游戏生成工作流执行完成");
            return Map.of();
        });
    }

    private List<Map<String, Object>> parseJsonQuestion(String llmResult) {
        try {
            String json = llmResult.trim();
            int left = json.indexOf("[");
            int right = json.lastIndexOf("]");
            if (left != -1 && right > left) {
                json = json.substring(left, right + 1);
            }
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("题目JSON解析失败", e);
            return Collections.emptyList();
        }
    }
}
