package com.woniuxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableJpaRepositories(basePackages = "com.woniuxy.repository")
public class AiEducation {
//    public static void main(String[] args) {
//        ConfigurableApplicationContext ctx = SpringApplication.run(AiEducation.class, args);
//        String aliKey = ctx.getEnvironment().getProperty("ALI_API_KEY");
//        String dashKey = ctx.getEnvironment().getProperty("DASHSCOPE_API_KEY");
//        System.out.println("ALI_API_KEY=[" + aliKey + "]");
//        System.out.println("DASHSCOPE_API_KEY=[" + dashKey + "]");
//    }

    public static void main(String[] args) {
        SpringApplication.run(AiEducation.class, args);
    }

}
