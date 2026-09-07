package com.woniuxy.agent;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.woniuxy.utils.DocumentParserUtils;
import com.woniuxy.utils.OssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocParserSubNodeFactory {
    private final DocParserAgentFactory docParserAgentFactory;
    private final OssUtil ossUtil;

    public StateGraph createGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                "docParserAgent", KeyStrategy.REPLACE,
                "document_raw_text", KeyStrategy.REPLACE,
                "clean_text", KeyStrategy.REPLACE,
                "doc_image_list", KeyStrategy.REPLACE
        );
        StateGraph graph = new StateGraph(keyStrategyFactory);
        addAllNode(graph);
        addAllEdge(graph);
        return graph;
    }

    private void addAllNode(StateGraph graph) throws GraphStateException {
        ReactAgent docAgent = docParserAgentFactory.DocParserAgent();
        graph.addNode("startNode", startNode());
        graph.addNode("docParserAgent", docAgent.asNode(true, false));
        graph.addNode("extractCleanText", extractCleanTextNode());
        graph.addNode("endNode", endNode());
    }

    private void addAllEdge(StateGraph graph) throws GraphStateException {
        graph.addEdge(StateGraph.START, "startNode");
        graph.addEdge("startNode", "docParserAgent");
        graph.addEdge("docParserAgent", "extractCleanText");
        graph.addEdge("extractCleanText", "endNode");
        graph.addEdge("endNode", StateGraph.END);
    }

    public AsyncNodeAction startNode() {
        return AsyncNodeAction.node_async(state -> {
            String docUrl = (String) state.value("docUrl")
                    .orElseThrow(() -> new RuntimeException("文档地址缺失，无法执行解析"));

            InputStream inputStream;
            String fileName;

            // 判断：OSS远程链接 / 本地文件路径
            if (docUrl.startsWith("https://")) {
                inputStream = ossUtil.downloadStreamByUrl(docUrl);
                // 截取文件名
                int idx = docUrl.lastIndexOf("/");
                fileName = idx > 0 ? docUrl.substring(idx + 1) : "temp.doc";
            } else {
                File docFile = new File(docUrl);
                if (!docFile.exists() || !docFile.isFile()) {
                    throw new RuntimeException("文档文件不存在：" + docUrl);
                }
                inputStream = new FileInputStream(docFile);
                fileName = docFile.getName();
            }

            byte[] fileBytes;
            try (InputStream is = inputStream) {
                fileBytes = is.readAllBytes();
            }

            // 1、提取文本
            String rawText;
            try (InputStream isText = new ByteArrayInputStream(fileBytes)) {
                rawText = DocumentParserUtils.extractText(isText, fileName);
            }
            if (rawText == null || rawText.isBlank()) {
                throw new RuntimeException("文档未能提取到有效文本");
            }

            // 2、提取内嵌图片
            List<byte[]> imageByteList;
            try {
                imageByteList = extractFileImageBytes(fileBytes);
                log.info("startNode 提取文档图片，数量={}", imageByteList.size());
            } catch (Exception e) {
                log.error("图片提取发生异常", e);
                imageByteList = Collections.emptyList();
            }

            // =====关键改动：byte[]转Base64字符串，不要把字节数组丢进state=====
            List<String> imgBase64List = new ArrayList<>();
            for (byte[] bytes : imageByteList) {
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                imgBase64List.add(base64);
            }

            return Map.of(
                    "document_raw_text", rawText,
                    "doc_image_list", imgBase64List  // 现在存 List<String> base64
            );
        });
    }

    // Tika提取图片二进制
    private List<byte[]> extractFileImageBytes(byte[] fileBytes) {
        List<byte[]> imgBytesList = new ArrayList<>();
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();
        ContentHandler emptyHandler = new DefaultHandler();

        parseContext.set(EmbeddedDocumentExtractor.class, new EmbeddedDocumentExtractor() {
            @Override
            public boolean shouldParseEmbedded(Metadata embedMeta) {
                return true;
            }

            @Override
            public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata embedMeta, boolean outputHtml) throws IOException {
                String contentType = embedMeta.get(Metadata.CONTENT_TYPE);
                String name = embedMeta.get("resourceName");
                boolean isImage = false;

                if (contentType != null && contentType.startsWith("image/")) {
                    isImage = true;
                }
                if (name != null) {
                    String lowerName = name.toLowerCase();
                    if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg")
                            || lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif")) {
                        isImage = true;
                    }
                }
                if (isImage) {
                    byte[] data = stream.readAllBytes();
                    if (data != null && data.length > 50) {
                        imgBytesList.add(data);
                    }
                }
            }
        });

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            parser.parse(inputStream, emptyHandler, metadata, parseContext);
        } catch (Exception e) {
            log.error("Tika解析图片异常", e);
        }
        return imgBytesList;
    }

    public AsyncNodeAction endNode() {
        return AsyncNodeAction.node_async(state -> Map.of());
    }

    public AsyncNodeAction extractCleanTextNode() {
        return AsyncNodeAction.node_async(state -> {
            AssistantMessage msg = (AssistantMessage) state.value("docParser").orElseThrow();
            String cleanText = msg.getText();
            return Map.of("clean_text", cleanText);
        });
    }
}