package com.finance.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 金融数据开放平台 - 启动类
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.finance.api.mapper")
public class FinanceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApiApplication.class, args);
    }
}
