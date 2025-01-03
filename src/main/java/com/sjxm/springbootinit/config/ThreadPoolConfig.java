package com.sjxm.springbootinit.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/2
 * @Description:
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor duplicateCheckThreadPool(){
        return new ThreadPoolExecutor(
                4,                      // 核心线程数
                8,                      // 最大线程数
                60L,                    // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNamePrefix("duplicate-check-thread-pool-").build()
        );
    }

}
