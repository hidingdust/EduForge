package com.woniuxy.entity.DTO;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DocParseResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件存储地址 OSS URL
     */
    private String fileOssUrl;

    /**
     * 文件原始名称
     */
    private String fileName;

    /**
     * 文件后缀：pdf/docx/md/txt
     */
    private String fileSuffix;

    /**
     * 文档全文文本
     */
    private String fullText;

    /**
     * 分片文本列表（向量化分片）
     */
    private List<String> textChunkList;

    /**
     * 向量库文档唯一 ID 列表
     */
    private List<String> vectorDocIdList;

    /**
     * 是否包含图片
     */
    private boolean hasImage;

    /**
     * OCR 识别文本
     */
    private String ocrText;
}