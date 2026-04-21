package com.finance.api.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 配置
 *
 * 包含：
 *   1. 分页插件 —— MyBatis-Plus 3.5.9 已内置分页，直接注册 MybatisPlusInterceptor 即可
 *   2. 字段自动填充（MetaObjectHandler）—— createTime / updateTime 自动写入
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatisPlus 插件链
     * 注意：3.5.9 版本 PaginationInnerInterceptor 已被移除，
     *       分页能力由 MybatisPlusInterceptor 内置，注册此 Bean 即可支持 Page 分页查询
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }

    /**
     * 自动填充创建/更新时间
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}

