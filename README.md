# Finance-API 金融数据开放平台

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

> 基于 Spring Boot 的金融数据开放平台，提供股票行情、自选股管理、价格提醒、资金流向等 RESTful API 服务。

## 📖 项目简介

本项目是一个面向金融场景的后端 API 服务，支持：
- **用户认证**：JWT 无状态认证，支持 Token 刷新
- **股票行情**：实时行情、历史K线、股票搜索（接入 NeoData）
- **自选股管理**：CRUD、标签管理、备注
- **价格提醒**：条件触发、RabbitMQ 异步通知
- **资金流向**：个股、北向资金、大盘、行业板块

## 🛠 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 核心框架 | Spring Boot 3.4.1 | Java 25，Jakarta EE |
| 安全认证 | Spring Security 6.5 + JWT | 无状态 Token 认证 |
| ORM | MyBatis-Plus 3.5.9 | 简化 CRUD，逻辑删除 |
| 缓存 | Redis (StringRedisTemplate) | 行情数据缓存，防抖限流 |
| 消息队列 | RabbitMQ | 异步通知，最终一致性 |
| API 文档 | Knife4j + Swagger | 在线接口文档 |
| 数据库 | MySQL 8.0 | finance_api 库 |
| 部署 | Docker Compose | 一键启动所有依赖（根目录 docker-compose.yml） |

## 🏗 项目架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端 (Apifox / 前端)                    │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP + JWT
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Security Filter Chain                  │
│  ┌─────────────────────┐    ┌─────────────────────────────────┐ │
│  │ JwtAuthenticationFilter │    │ AuthorizationFilter           │ │
│  │ - 解析 JWT Token      │    │ - 检查 .authenticated()        │ │
│  │ - 注入 SecurityContext│    │ - 拒绝未认证请求                │ │
│  └─────────────────────┘    └─────────────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                    ┌───────────┴───────────┐
                    ▼                       ▼
┌─────────────────────────┐    ┌─────────────────────────┐
│      Controller 层        │    │    @Scheduled 定时任务   │
│  - AuthController        │    │  - PriceMonitorTask     │
│  - StockController       │    │    每分钟检查价格提醒     │
│  - WatchlistController   │    │                         │
│  - MoneyFlowController   │    │                         │
└────────────┬──────────────┘    └────────────┬────────────┘
             │                               │
             ▼                               ▼
┌─────────────────────────┐    ┌─────────────────────────┐
│       Service 层          │    │     RabbitMQ             │
│  - StockQuoteService     │    │  ┌───────────────────┐   │
│    + Redis 缓存 (30s)    │───▶│  │ PriceAlertConsumer │   │
│  - MoneyFlowService      │    │  │ 异步发送通知       │   │
│  - WatchlistService      │    │  └───────────────────┘   │
└────────────┬──────────────┘    └─────────────────────────┘
             │
             ▼
┌─────────────────────────┐    ┌─────────────────────────┐
│       NeoData API        │    │       MySQL 8.0          │
│  - 实时行情 (rt_k)       │    │  - sys_user            │
│  - 历史日线 (daily)      │    │  - stock_watchlist     │
│  - 资金流向 (moneyflow)  │    │  - price_alert         │
└─────────────────────────┘    └─────────────────────────┘
```

## 📁 项目结构

```
finance-api/
├── src/main/java/com/finance/api/
│   ├── config/               # 配置类
│   │   ├── SecurityConfig.java       # Spring Security 配置
│   │   ├── JwtAuthenticationFilter.java  # JWT 认证过滤器
│   │   ├── RedisConfig.java          # Redis 序列化配置
│   │   ├── RabbitMQConfig.java       # MQ 交换机/队列配置
│   │   └── SwaggerConfig.java        # API 文档配置
│   ├── controller/           # 控制器层
│   │   ├── AuthController.java        # 认证（登录/注册）
│   │   ├── StockController.java       # 股票行情
│   │   ├── WatchlistController.java   # 自选股管理
│   │   └── MoneyFlowController.java   # 资金流向
│   ├── service/              # 业务逻辑层
│   │   ├── impl/
│   │   │   ├── StockQuoteServiceImpl.java  # NeoData API 接入
│   │   │   ├── MoneyFlowServiceImpl.java   # 资金流向服务
│   │   │   └── ...
│   │   ├── PriceMonitorTask.java     # 价格监控定时任务
│   │   └── PriceAlertConsumer.java   # MQ 消息消费者
│   ├── entity/               # 数据实体
│   ├── dto/                  # 请求 DTO
│   ├── vo/                   # 响应 VO
│   ├── mapper/               # MyBatis Mapper
│   └── util/                 # 工具类
│       ├── JwtUtil.java            # JWT 工具
│       └── UserContext.java        # ThreadLocal 用户上下文
├── src/main/resources/
│   ├── application.yml             # 主配置
│   ├── logback-spring.xml          # 日志配置
│   └── db/
│       ├── schema.sql              # 数据库表结构
│       └── data.sql                # 测试数据
├── docker-compose.yml              # Docker 一键部署（完整服务）
└── pom.xml                        # Maven 依赖
```

## 🚀 快速开始

### 环境要求

- JDK 25+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.x
- Maven 3.8+

### 1. 启动依赖服务（Docker）

```bash
# 根目录 docker-compose.yml 包含完整服务（MySQL + Redis + RabbitMQ + API）
docker-compose up -d

# 仅启动中间件（不包含 API 服务，用于本地开发时）
docker-compose up -d mysql redis rabbitmq
```

### 2. 初始化数据库

```bash
# 连接 MySQL
mysql -u root -p

# 执行初始化脚本
source src/main/resources/db/schema.sql;
source src/main/resources/db/data.sql;
```

### 3. 启动应用

```bash
mvn spring-boot:run
# 或直接运行 FinanceApiApplication.java
```

### 4. 访问 API 文档

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Knife4j: http://localhost:8080/doc.html

## 🔐 认证流程

### 1. 登录获取 Token

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456"
}
```

### 2. 携带 Token 访问接口

```http
GET /api/stocks/realtime?tsCode=600519.SH
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 2
```

## 📊 API 一览

| 模块 | 路径 | 方法 | 认证 | 说明 |
|------|------|:----:|:----:|------|
| **认证** | `/api/auth/login` | POST | ❌ | 用户登录 |
| | `/api/auth/register` | POST | ❌ | 用户注册 |
| | `/api/auth/refresh` | POST | ❌ | 刷新 Token |
| **股票** | `/api/stocks/realtime` | GET | ✅ | 实时行情 |
| | `/api/stocks/daily` | GET | ✅ | 历史K线 |
| | `/api/stocks/search` | GET | ✅ | 股票搜索 |
| **自选股** | `/api/watchlist` | GET | ✅ | 自选股列表 |
| | `/api/watchlist/add` | POST | ✅ | 添加自选 |
| | `/api/watchlist/remove/{id}` | DELETE | ✅ | 删除自选 |
| **提醒** | `/api/watchlist/alerts` | GET | ✅ | 提醒列表 |
| | `/api/watchlist/alerts` | POST | ✅ | 创建提醒 |
| **资金** | `/api/moneyflow/stock/{tsCode}` | GET | ✅ | 个股资金 |
| | `/api/moneyflow/hsgt` | GET | ✅ | 北向资金 |
| | `/api/moneyflow/market` | GET | ✅ | 大盘资金 |

## 💡 技术亮点（面试重点）

### 1. JWT + Spring Security Filter Chain

**问题**：原架构使用 MVC HandlerInterceptor 解析 JWT，但 Spring Security Filter 先于 Interceptor 执行，导致 `.authenticated()` 规则在 Token 解析之前就拦截请求。

**解决**：新建 `JwtAuthenticationFilter`（OncePerRequestFilter），通过 `addFilterBefore` 插入 Spring Security 过滤链。

```java
// SecurityConfig.java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

**过滤器执行顺序**：
1. `JwtAuthenticationFilter` - 解析 JWT，写入 SecurityContext
2. `AuthorizationFilter` - 检查授权规则

### 2. Redis 多级缓存策略

**行情缓存**：
- 实时行情：30 秒缓存
- 历史K线：5 分钟缓存
- 搜索结果：1 小时缓存

**降级方案**：当 `rt_k` 接口限流时，自动降级为 `daily` 接口获取最新数据。

### 3. RabbitMQ 异步通知

**价格提醒触发流程**：
```
定时任务 (每分钟)
    │
    ▼
查询启用的提醒 ──▶ 判断条件 ──▶ 更新数据库 ──▶ 发送 MQ 消息
                                            │
                                            ▼
                                      PriceAlertConsumer
                                            │
                                            ▼
                                      异步发送通知
```

**好处**：定时任务快速返回，不阻塞；通知失败不影响主业务。

### 4. ThreadLocal 用户上下文

**为什么需要**：
- Filter 解析 Token 后，需要把 userId 传递给 Controller/Service
- 但 Filter 和 Controller 之间没有直接的参数传递机制

**解决方案**：
```java
// JwtAuthenticationFilter
UserContext.setUserId(userId);

// Controller
Long userId = UserContext.getUserId();

// JwtInterceptor.afterCompletion
UserContext.clear(); // 防止内存泄漏
```

### 5. MyBatis-Plus 逻辑删除

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

所有查询自动拼接 `WHERE deleted = 0`，无需手动处理。

## 📋 数据库表结构

### sys_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(255) | BCrypt 加密 |
| nickname | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 |
| status | TINYINT | 状态（0禁用/1正常） |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |

### stock_watchlist（自选股表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| stock_code | VARCHAR(20) | 股票代码 |
| stock_name | VARCHAR(50) | 股票名称 |
| market | VARCHAR(10) | 市场（SH/SZ/HK/US） |
| tags | VARCHAR(100) | 标签 |

### price_alert（价格提醒表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| stock_code | VARCHAR(20) | 股票代码 |
| target_price | DECIMAL(10,2) | 目标价格 |
| alert_type | VARCHAR(20) | gt/lt/eq |
| is_triggered | TINYINT | 是否已触发 |
| is_enabled | TINYINT | 是否启用 |

## ⚙️ 配置说明

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| DB_HOST | localhost | MySQL 地址 |
| DB_PORT | 3306 | MySQL 端口 |
| DB_USER | root | 数据库用户 |
| DB_PWD | root123 | 数据库密码 |
| REDIS_HOST | localhost | Redis 地址 |
| REDIS_PORT | 6379 | Redis 端口 |
| REDIS_PWD | 123456 | Redis 密码 |
| RABBITMQ_HOST | localhost | RabbitMQ 地址 |
| RABBITMQ_PORT | 5672 | RabbitMQ 端口 |
| JWT_SECRET | ... | JWT 密钥（生产环境必须修改） |

### JWT 配置

```yaml
jwt:
  ttl-hours: 24              # Access Token 有效期（小时）
  refresh-ttl-hours: 168      # Refresh Token 有效期（小时）
```

## 📝 测试账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| admin | 123456 | 管理员 |
| testuser | 123456 | 测试用户 |
| trader01 | 123456 | 交易员 |

## 🔧 开发说明

### 编译

```bash
mvn clean compile
```

### 运行测试

```bash
mvn test
```

### 打包

```bash
mvn clean package -DskipTests
```

## 📚 参考资料

- [Spring Security 6 官方文档](https://spring.io/projects/spring-security)
- [MyBatis-Plus 官方文档](https://baomidou.com/pages/24112f/)
- [JJWT 使用指南](https://github.com/jwtk/jjwt)
- [NeoData 金融数据 API](https://www.neodot.cn/)

## 📄 License

MIT License
