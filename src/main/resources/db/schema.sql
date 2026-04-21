-- =====================================================
-- 金融数据开放平台 - 数据库初始化脚本
-- =====================================================
CREATE DATABASE IF NOT EXISTS finance_api DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE finance_api;

-- =====================================================
-- 1. 用户表
-- =====================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- =====================================================
-- 2. 自选股表
-- =====================================================
DROP TABLE IF EXISTS `stock_watchlist`;
CREATE TABLE `stock_watchlist` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `stock_code` VARCHAR(20) NOT NULL COMMENT '股票代码（如 000001.SZ）',
    `stock_name` VARCHAR(50) NOT NULL COMMENT '股票名称',
    `market` VARCHAR(10) NOT NULL COMMENT '市场（SH/SZ/HK/US）',
    `tags` VARCHAR(100) DEFAULT NULL COMMENT '标签，逗号分隔',
    `note` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_stock` (`user_id`, `stock_code`, `market`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_stock_code` (`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自选股表';

-- =====================================================
-- 3. 价格提醒表
-- =====================================================
DROP TABLE IF EXISTS `price_alert`;
CREATE TABLE `price_alert` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `stock_code` VARCHAR(20) NOT NULL COMMENT '股票代码',
    `stock_name` VARCHAR(50) NOT NULL COMMENT '股票名称',
    `target_price` DECIMAL(10, 2) NOT NULL COMMENT '目标价格',
    `alert_type` VARCHAR(20) NOT NULL COMMENT '提醒类型：gt-大于 lt-小于 eq-等于',
    `condition_desc` VARCHAR(100) DEFAULT NULL COMMENT '条件描述',
    `is_triggered` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已触发：0-否 1-是',
    `triggered_at` DATETIME DEFAULT NULL COMMENT '触发时间',
    `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_stock_code` (`stock_code`),
    KEY `idx_is_enabled_triggered` (`is_enabled`, `is_triggered`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格提醒表';

-- =====================================================
-- 4. 操作日志表（可选，记录关键操作）
-- =====================================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户 ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    `module` VARCHAR(50) DEFAULT NULL COMMENT '模块',
    `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `uri` VARCHAR(500) DEFAULT NULL COMMENT '请求 URI',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP 地址',
    `result` TEXT DEFAULT NULL COMMENT '返回结果摘要',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-失败 1-成功',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '耗时（毫秒）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =====================================================
-- 初始管理员账号（密码: admin123）
-- BCrypt 加密后的密码
-- =====================================================
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `email`, `status`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '超级管理员', 'admin@finance-api.com', 1);
