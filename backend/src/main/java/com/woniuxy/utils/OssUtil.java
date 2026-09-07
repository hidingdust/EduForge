package com.woniuxy.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssUtil {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    // 全局单例OSS客户端，容器启动初始化一次
    private OSS ossClient;

    @PostConstruct
    public void initClient() {
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    @PreDestroy
    public void destroyClient() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    /**
     * 流上传（自定义目录）
     * @param inputStream 文件流
     * @param dir 存储目录，例如 ai_ppt / ai_work_doc / ai_animation
     * @param fileName 文件名
     * @return 外网访问URL
     */
    public String uploadFile(InputStream inputStream, String dir, String fileName) {
        // 保证目录末尾带 /
        String baseDir = dir.endsWith("/") ? dir : dir + "/";
        String savePath = baseDir + System.currentTimeMillis() + "_" + fileName;
        ossClient.putObject(new PutObjectRequest(bucketName, savePath, inputStream));
        return buildUrl(savePath);
    }

    /**
     * 字节数组上传（专门给POI内存生成文件使用，Word/PPT/动画二进制）
     * @param bytes 文件字节数组
     * @param dir 存储目录
     * @param fileName 文件名
     * @return 外网访问URL
     */
    public String uploadBytes(byte[] bytes, String dir, String fileName) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            return uploadFile(is, dir, fileName);
        }
    }

    /**
     * 【兼容旧代码】原有PPT上传，默认存入ai_ppt目录
     */
    @Deprecated
    public String uploadFile(InputStream inputStream, String fileName) {
        return uploadFile(inputStream, "ai_ppt", fileName);
    }

    /**
     * 本地文件上传（自定义目录）
     */
    public String uploadLocalFile(File file, String dir) {
        String baseDir = dir.endsWith("/") ? dir : dir + "/";
        String savePath = baseDir + "output_" + System.currentTimeMillis() + "_" + file.getName();
        ossClient.putObject(new PutObjectRequest(bucketName, savePath, file));
        return buildUrl(savePath);
    }

    /**
     * 【兼容旧代码】默认ai_ppt目录
     */
    @Deprecated
    public String uploadLocalFile(File file) {
        return uploadLocalFile(file, "ai_ppt");
    }

    /**
     * 根据完整URL下载文件流
     */
    public InputStream downloadStreamByUrl(String fileUrl) {
        String baseUrl = "https://" + bucketName + "." + endpoint + "/";
        String objectName = fileUrl.replace(baseUrl, "");
        GetObjectRequest request = new GetObjectRequest(bucketName, objectName);
        return ossClient.getObject(request).getObjectContent();
    }

    // 拼接OSS外网访问地址
    private String buildUrl(String objectPath) {
        return String.format("https://%s.%s/%s", bucketName, endpoint, objectPath);
    }
    public String uploadMultipartFile(MultipartFile multipartFile, String dir) throws IOException {
        String originalFilename = multipartFile.getOriginalFilename();
        try (InputStream inputStream = multipartFile.getInputStream()) {
            return uploadFile(inputStream, dir, originalFilename);
        }
    }
}