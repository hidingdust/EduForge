package com.woniuxy.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    //我的
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(){
        //Redisson配置对象，用来连接Redis
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        //返回创建Redisson客户端实例，建立与Redis的连接
        return Redisson.create(config);
    }
}