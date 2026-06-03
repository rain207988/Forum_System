# Forum AOP 日志增强说明

## 1. 这轮增强做了什么

这次不是只“加一个 AOP 注解”，而是把项目的日志可观测性往上线项目常见做法推进了一步：

- 引入 Spring AOP
- 增加统一的 Controller 调用日志
- 增加 Service 异常日志和慢调用告警
- 增加全链路 `traceId`
- 自动把 `traceId` 回写到响应头
- 对密码、token、secret 这类敏感参数做日志脱敏

---

## 2. 当前项目之前有没有 AOP

这轮改造前，项目里：

- 没有 `@Aspect`
- 没有统一方法切面
- 没有 `spring-boot-starter-aop`

也就是说，之前的日志主要还是散落在各个 Controller / Service 里的手写 `log.info(...)`。

这种方式在小项目里够用，但到排障、压测、线上定位问题时会比较吃力。

---

## 3. 这轮新增了哪些能力

### 3.1 引入 AOP 依赖

修改文件：

- [pom.xml](D:/code_java/forum/Forum_System/forum/pom.xml)

新增依赖：

- `spring-boot-starter-aop`

作用：

- 给项目补齐统一切面能力

---

### 3.2 新增统一日志切面

新增文件：

- [src/main/java/com/xzy/forum/aop/ApplicationLoggingAspect.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/aop/ApplicationLoggingAspect.java)

这部分主要做了两类增强：

1. Controller 调用日志
2. Service 异常与慢调用日志

#### Controller 日志

对 `@RestController` 统一记录：

- `traceId`
- HTTP 方法
- 请求路径
- 当前登录用户 `userId`
- 处理方法
- 参数摘要
- 返回摘要
- 耗时

日志标识：

- `[controller-access]`
- `[controller-error]`

#### Service 日志

对 `services.impl` 和 `service` 包下公开方法统一增强：

- 发生异常时记录错误日志
- 超过阈值时记录慢调用告警

日志标识：

- `[service-error]`
- `[service-slow]`

这样比单纯在业务代码里手写日志更统一，也更容易全局治理。

---

### 3.3 敏感参数自动脱敏

切面会自动识别参数名里包含以下关键字的字段：

- `password`
- `token`
- `authorization`
- `secret`
- `salt`

这些值在日志里统一显示为：

- `***`

这样可以明显降低把敏感信息打进日志的风险。

---

### 3.4 新增 TraceId 过滤器

新增文件：

- [src/main/java/com/xzy/forum/filter/TraceIdFilter.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/filter/TraceIdFilter.java)
- [src/main/java/com/xzy/forum/config/ForumObservabilityProperties.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/ForumObservabilityProperties.java)

能力包括：

- 如果请求头里已有 `X-Trace-Id`，直接沿用
- 如果没有，就自动生成一个新的 traceId
- 写入 `MDC`
- 写入请求属性
- 回写到响应头

这样前后端联调、接口压测、线上排障时，就能通过 `traceId` 把一次请求串起来。

---

### 3.5 日志输出自动带 traceId

修改文件：

- [src/main/resources/application.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application.yml)

新增日志级别格式：

- `logging.pattern.level: "%5p [traceId:%X{traceId:-N/A}]"`

效果：

- 所有日志都会带上当前请求上下文中的 `traceId`

---

## 4. 这轮还能增强了什么

除了 AOP 本身，这次顺手补了两类更有上线价值的增强：

### 4.1 慢调用告警

配置项：

- `forum.observability.slow-service-threshold-ms`

默认值：

- `300ms`

超过阈值时，Service 会打出 `[service-slow]` 告警日志。

这对后续发现慢 SQL、慢 Redis、慢外部调用很有帮助。

### 4.2 响应头回传 TraceId

响应头里现在会返回：

- `X-Trace-Id`

这样前端、测试人员、接口调用方都可以拿着这个值去日志里查问题。

---

## 5. 修改了哪些文件

新增文件：

- [src/main/java/com/xzy/forum/aop/ApplicationLoggingAspect.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/aop/ApplicationLoggingAspect.java)
- [src/main/java/com/xzy/forum/filter/TraceIdFilter.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/filter/TraceIdFilter.java)
- [src/main/java/com/xzy/forum/config/ForumObservabilityProperties.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/ForumObservabilityProperties.java)
- [src/test/java/com/xzy/forum/observability/ObservabilityIntegrationTest.java](D:/code_java/forum/Forum_System/forum/src/test/java/com/xzy/forum/observability/ObservabilityIntegrationTest.java)
- [AOP_LOGGING_UPGRADE.md](D:/code_java/forum/Forum_System/forum/AOP_LOGGING_UPGRADE.md)

修改文件：

- [pom.xml](D:/code_java/forum/Forum_System/forum/pom.xml)
- [src/main/java/com/xzy/forum/config/AppConfig.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/AppConfig.java)
- [src/main/resources/application.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application.yml)

---

## 6. 自动化验证

新增测试：

- [src/test/java/com/xzy/forum/observability/ObservabilityIntegrationTest.java](D:/code_java/forum/Forum_System/forum/src/test/java/com/xzy/forum/observability/ObservabilityIntegrationTest.java)

覆盖了两个关键点：

1. `traceId` 响应头能正确回传
2. AOP 日志会输出 Controller 访问日志，并且敏感参数会被脱敏

---

## 7. 建议如何验证

```powershell
.\mvnw.cmd "-Dtest=ObservabilityIntegrationTest,ForumApplicationTests" test
```

如果要走完整回归：

```powershell
.\mvnw.cmd test
```

---

## 8. 这轮增强后的直接收益

这轮完成后，项目在日志和排障能力上明显更像团队项目：

- 请求链路有 `traceId`
- Controller 日志统一
- Service 异常日志统一
- 慢调用会被自动告警
- 敏感字段不会直接打进日志

这类增强对“正式上线后能不能快速定位问题”很重要，实际价值通常比多加一个小功能更高。
