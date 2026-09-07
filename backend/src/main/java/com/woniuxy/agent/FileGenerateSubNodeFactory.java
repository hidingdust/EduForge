package com.woniuxy.agent;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woniuxy.service.impl.DocToPptImpl.DTO.PptPageDTO;
import com.woniuxy.service.impl.DocToPptImpl.DTO.PptTotalDTO;
import com.woniuxy.utils.OssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileGenerateSubNodeFactory {
    private final OssUtil ossUtil;
    private final ObjectMapper objectMapper;
    // RestTemplate 暂时保留，后续不需要可以一并删除
    private final RestTemplate restTemplate;

    // ============ 状态输出Key定义 ============
    public static final String PPT_DOWNLOAD_URL = "pptDownloadUrl";

    // 输入状态需要携带的数据key
    public static final String PPT_JSON = "PPT_JSON";
    public static final String TASK_ID = "taskId";

    public StateGraph createGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                PPT_DOWNLOAD_URL, KeyStrategy.REPLACE
        );
        StateGraph graph = new StateGraph(keyStrategyFactory);
        buildGraphNodes(graph);
        buildGraphEdge(graph);
        return graph;
    }

    private void buildGraphNodes(StateGraph graph) throws GraphStateException {
        graph.addNode("startNode", AsyncNodeAction.node_async(state -> Map.of()));
        graph.addNode("generatePptNode", generatePptNode());
        graph.addNode("endNode", AsyncNodeAction.node_async(state -> Map.of()));
    }

    private void buildGraphEdge(StateGraph graph) throws GraphStateException {
        graph.addEdge(StateGraph.START, "startNode");
        graph.addEdge("startNode", "generatePptNode");
        graph.addEdge("generatePptNode", "endNode");
        graph.addEdge("endNode", StateGraph.END);
    }

    /**
     * PPT_JSON -> 生成pptx二进制 + 上传OSS
     */
    private AsyncNodeActionWithConfig generatePptNode() {
        return AsyncNodeActionWithConfig.node_async((state, config) -> {
            if (state.value(PPT_JSON).isEmpty()) {
                log.info("当前任务无PPT_JSON，跳过PPT文件生成");
                return Map.of();
            }
            String taskId = state.value(TASK_ID, String.class).orElse("");
            Object pptJsonObj = state.value(PPT_JSON).get();
            String pptJsonStr = pptJsonObj.toString();

            byte[] pptBytes = buildPptFromJson(pptJsonStr);
            String fileName = taskId + "_course.pptx";
            String url = ossUtil.uploadBytes(pptBytes, "ai_pptx", fileName);
            log.info("PPT文件生成上传成功 taskId:{} url={}", taskId, url);
            return Map.of(PPT_DOWNLOAD_URL, url);
        });
    }

    // ====================== 私有工具方法 ======================
    /**
     * PPT JSON 转 PptTotalDTO，循环pages生成每页幻灯片
     */
    private byte[] buildPptFromJson(String pptJson) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            PptTotalDTO pptDto = objectMapper.readValue(pptJson, PptTotalDTO.class);

            for (PptPageDTO page : pptDto.getPages()) {
                XSLFSlide slide = ppt.createSlide();

                // 绘制标题
                XSLFTextShape titleBox = slide.createTextBox();
                titleBox.setAnchor(new Rectangle(50, 30, 600, 80));
                titleBox.setText(page.getPageTotal());

                // 绘制正文
                XSLFTextShape contentBox = slide.createTextBox();
                contentBox.setAnchor(new Rectangle(50, 130, 600, 300));
                contentBox.setText(page.getPageContent());
            }

            ppt.write(bos);
            byte[] pptFile = bos.toByteArray();
            log.info("PPT组装完毕，文件大小：{}", pptFile.length);
            return pptFile;
        }
    }
}