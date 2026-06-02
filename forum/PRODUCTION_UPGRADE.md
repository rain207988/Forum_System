# Forum 生产化增强说明

## 这次补了什么

这轮主要补的是更接近正式上线要求的基础能力，不是单纯新增业务功能。

已经完成：

- 登录、注册、站内信发送/回复的限流防滥用
- 可配置的 CORS，而不是全量放开
- 安全响应头
- 生产环境关闭 Swagger 和测试接口
- graceful shutdown、压缩、转发头支持
- Dockerfile、docker-compose、`.env.example`
- `logs/`、`data/` 运行产物从版本库移除并忽略

## 为什么优先做这些

这些点是项目上线最容易出事故的地方：

- 没有限流，登录和注册很容易被刷
- CORS 全放开，前端来源不可控
- 测试接口暴露在生产环境，不符合上线要求
- 没有容器化交付文件，部署成本高
- 运行日志和本地数据库文件进入 Git，会污染主分支

## 具体改动

### 1. 限流

新增：

- [RateLimitService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/service/RateLimitService.java)
- [ForumRateLimitProperties.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/ForumRateLimitProperties.java)

接入位置：

- [UserController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/UserController.java)
- [MessageController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/MessageController.java)

策略：

- 登录：默认 60 秒最多 10 次
- 注册：默认 1 小时最多 5 次
- 站内信发送：默认 60 秒最多 20 次
- 站内信回复：默认 60 秒最多 30 次

实现方式：

- Redis 可用时优先走 Redis 计数
- Redis 不可用时自动退化到本地内存限流

### 2. CORS 收敛

原来：

- `allowedOriginPatterns("*")`

现在：

- 改为走配置项 `forum.cors.allowed-origin-patterns`
- 默认本地环境只放行 `localhost/127.0.0.1`
- 生产环境默认只放行 `https://forum.example.com`

文件：

- [WebMvcConfig.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/WebMvcConfig.java)
- [ForumCorsProperties.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/ForumCorsProperties.java)

### 3. 安全响应头

新增：

- [SecurityHeadersFilter.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/filter/SecurityHeadersFilter.java)
- [ForumSecurityHeadersProperties.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/ForumSecurityHeadersProperties.java)

默认增加：

- `X-Content-Type-Options`
- `X-Frame-Options`
- `Referrer-Policy`
- `Permissions-Policy`
- `Cross-Origin-Opener-Policy`
- `Content-Security-Policy`
- HTTPS / 反代 HTTPS 下自动加 `HSTS`

### 4. 生产环境关闭 Swagger 和测试接口

原来：

- `/test/**` 可直接访问
- Swagger 默认开启

现在：

- `forum.features.test-api-enabled=false` 时，不注册测试接口
- `forum.features.swagger-enabled=false` 时，不注册 Swagger 配置
- `application-prod.yml` 默认关闭两者

文件：

- [TestController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/TestController.java)
- [SwaggerConfig.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/SwaggerConfig.java)
- [ForumFeatureProperties.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/ForumFeatureProperties.java)

### 5. 部署和运维配置

增强了：

- `server.shutdown=graceful`
- `spring.lifecycle.timeout-per-shutdown-phase=30s`
- 响应压缩
- `forward-headers-strategy=framework`
- 生产环境只暴露 `health`、`info`

文件：

- [application.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application.yml)
- [application-prod.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application-prod.yml)

### 6. Docker 化交付

新增：

- [Dockerfile](D:/code_java/forum/Forum_System/forum/Dockerfile)
- [docker-compose.yml](D:/code_java/forum/Forum_System/forum/docker-compose.yml)
- [.env.example](D:/code_java/forum/Forum_System/forum/.env.example)

用途：

- 本地或服务器可以直接用容器启动
- MySQL、Redis、应用三者可以一套编排启动

### 7. Git 运行产物治理

新增忽略：

- `logs/`
- `data/`
- `.env`
- `.env.local`

并把已被 Git 跟踪的本地运行文件移出版本库索引。

文件：

- [.gitignore](D:/code_java/forum/Forum_System/forum/.gitignore)

## 新的环境变量

至少需要这些：

- `FORUM_DB_URL`
- `FORUM_DB_USERNAME`
- `FORUM_DB_PASSWORD`
- `FORUM_REDIS_HOST`
- `FORUM_REDIS_PORT`
- `FORUM_REDIS_PASSWORD`
- `FORUM_JWT_SECRET`
- `FORUM_CORS_ALLOWED_ORIGIN_PATTERNS`

可直接参考：

- [.env.example](D:/code_java/forum/Forum_System/forum/.env.example)

## 怎么启动

### 方式 1：本地 Java 启动

生产配置：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

### 方式 2：Docker Compose

先复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

然后修改 `.env` 里的真实密码和域名，再执行：

```powershell
docker compose up -d --build
```

## 已完成验证

已通过：

```powershell
.\mvnw.cmd test
```

已通过本地 MySQL 消息链路验证：

```powershell
.\mvnw.cmd test "-Dforum.mysql.it=true"
```

## 下一步还建议继续补

如果继续往真正上线推进，下一批建议做：

1. `refresh token`
2. 登录验证码 / 图形验证码
3. 审计日志和登录日志
4. 文件上传和对象存储
5. WebSocket 实时站内信
6. Nginx 反向代理配置
7. 数据库迁移脚本工具，例如 Flyway
