package com.woniuxy.utils;
import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import org.springframework.ai.document.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class DocumentParserUtils {

    /**
     * 文档文本提取入口
     * @param inputStream 文件流
     * @param fileName 文件名称（后缀用于辅助识别，用于日志/异常提示）
     * @return 原始未清洗文本
     */
    public static String extractText(InputStream inputStream, String fileName) {
        byte[] fileBytes;
        try {
            // 一次性读取全部字节，规避MultipartFile流只能读取一次的致命问题
            fileBytes = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("读取文件流失败，文件名：" + fileName, e);
        }

        TikaDocumentParser parser = new TikaDocumentParser();
        // 使用字节数组新建流，可安全交给解析器
        try (InputStream bis = new ByteArrayInputStream(fileBytes)) {
            List<Document> documentList = parser.parse(bis);
            return documentList.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败，文件名：" + fileName, e);
        }
    }
}