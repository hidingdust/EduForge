package com.woniuxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 本地图片存放的根路径，建议放到配置文件里
    @Value("${file.upload.path:C:/Users/lenovo/Pictures/壁纸/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 访问 /files/xxx.jpg 就会映射到本地文件夹
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadPath);
    }
}