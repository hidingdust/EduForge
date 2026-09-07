package com.woniuxy.agent;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.woniuxy.service.impl.DocToPptImpl.DTO.PptTotalDTO;
import com.woniuxy.service.impl.DocToPptImpl.VO.PptVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class Doc2PptSubNodeFactory {
    private final DocParserAgentFactory docParserAgentFactory;

    public static final String PPT_JSON = "PPT_JSON";
    public static final String PPT_DTO = "PPT_DTO";
    public static final String PPT_VO = "PPT_VO";

    // 和DocParserSubNodeFactory保持一致入口
    public StateGraph createGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                PPT_JSON, KeyStrategy.REPLACE,
                PPT_DTO, KeyStrategy.REPLACE,
                PPT_VO, KeyStrategy.REPLACE
        );
        StateGraph graph = new StateGraph(keyStrategyFactory);
        addAllNode(graph);
        addAllEdge(graph);
        return graph;
    }

    private void addAllNode(StateGraph graph) throws GraphStateException {
        // 只有你这一个业务节点
        ReactAgent doc2PPTAgent=docParserAgentFactory.doc2PPTAgent();
        graph.addNode("startNode", AsyncNodeAction.node_async(state -> Map.of()));
        graph.addNode("doc2PPTAgent",doc2PPTAgent.asNode(true,false));
        graph.addNode("jsonToDtoNode", jsonToDtoNode());
        graph.addNode("endNode", AsyncNodeAction.node_async(state -> Map.of()));
    }

    private void addAllEdge(StateGraph graph) throws GraphStateException {
        graph.addEdge(StateGraph.START, "startNode");
        graph.addEdge("startNode", "doc2PPTAgent");
        graph.addEdge("doc2PPTAgent", "jsonToDtoNode");
        graph.addEdge("jsonToDtoNode", "endNode");
        graph.addEdge("endNode", StateGraph.END);
    }

    // 你的业务节点逻辑，完全保留原来代码不动
    public AsyncNodeActionWithConfig jsonToDtoNode() {
        return AsyncNodeActionWithConfig.node_async((state, config) -> {
            // state中doc_image_list现在是List<String> base64，不再是byte[]
            List<String> docImageList = (List<String>) state.value("doc_image_list")
                    .orElse(Collections.emptyList());
            log.info("PPT子图拿到图片数量={}", docImageList.size());

            Object msgObj = state.value(PPT_JSON).orElseThrow(() -> new RuntimeException("AI未返回PPT结构化数据"));
            String raw = msgObj instanceof AssistantMessage am ? am.getText().trim() : msgObj.toString().trim();
            String json = raw.replaceAll("[\\n\\t ]", "").replaceAll("^```json|^```|```$", "").trim();

            try {
                PptTotalDTO dto = JSON.parseObject(json, PptTotalDTO.class);
                if(dto.getPages() == null) dto.setPages(Collections.emptyList());
                PptVO vo = new PptVO();
                BeanUtils.copyProperties(dto, vo);

                // ✅补回PPT_JSON，给Supervisor监督者做完成标记
                return Map.of(
                        PPT_JSON, json,
                        PPT_DTO, dto,
                        PPT_VO, vo
                );
            } catch (JSONException e) {
                log.error("PPT JSON解析失败，原始字符串：{}", json, e);
                throw new RuntimeException("PPT结构化数据解析异常", e);
            }
        });
    }
}