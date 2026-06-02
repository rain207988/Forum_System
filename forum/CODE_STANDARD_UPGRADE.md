# Forum 代码规范整改说明

## 本次整改目标

这轮工作聚焦在“更接近公司级代码规范”的几类问题上，不继续堆功能，而是优先处理会影响维护性、稳定性和安全默认值的代码：

- 依赖注入方式不统一，仍有字段注入残留
- 认证拦截链路对非法 token 的兜底不够稳定
- 文章浏览数、点赞数、回复数使用读改写，存在并发丢计数风险
- 默认配置里测试接口暴露、数据库密码存在明文默认值
- 少量服务代码存在旧式校验和日志写法

## 实际修改内容

### 1. 统一部分核心类为构造器注入

修改文件：

- `src/main/java/com/xzy/forum/controller/ArticleReplyController.java`
- `src/main/java/com/xzy/forum/services/impl/ArticleReplyServiceImpl.java`

修改方式：

- 移除字段注入
- 依赖改为 `private final`
- 使用构造器显式声明依赖

为什么改：

- 字段注入隐藏依赖，不利于 code review
- 构造器注入更利于测试和重构
- `final` 依赖能降低误改和空注入风险

修改后的好处：

- 依赖关系一眼可见
- 更符合团队常见 Spring 编码规范
- 后续做单测或重构更方便

### 2. 修复认证拦截器对非法 token 的异常兜底

修改文件：

- `src/main/java/com/xzy/forum/interceptor/LoginInterceptor.java`
- `src/test/java/com/xzy/forum/controller/UserControllerAuthIntegrationTest.java`

修改方式：

- 在拦截器里捕获 `ApplicationException`
- 非法/过期/损坏 token 统一转换为 `401 Unauthorized`
- 增加一条非法 access token 的集成测试

为什么改：

- 之前这类异常可能直接冒到全局异常处理，行为不够稳定
- 鉴权失败应该在拦截层就被明确识别，而不是表现成通用服务异常

修改后的好处：

- 前后端对登录态失效的处理更稳定
- 更符合网关/鉴权中间层的职责边界
- 线上排障时更容易区分“未授权”和“系统异常”

### 3. 将文章计数更新改为数据库原子自增

修改文件：

- `src/main/java/com/xzy/forum/dao/ArticleMapper.java`
- `src/main/resources/mapper/ArticleMapper.xml`
- `src/main/java/com/xzy/forum/services/impl/ArticleServiceImpl.java`

修改方式：

- 新增 `incrementVisitCountById`
- 新增 `incrementLikeCountById`
- 新增 `incrementReplyCountById`
- 服务层从“先查再写”改成直接执行数据库自增

为什么改：

- 原来的读改写方式在并发下会发生覆盖，导致浏览数、点赞数、回复数丢失
- 这类问题本地单人测试很难看出来，但线上很常见

修改后的好处：

- 降低并发丢计数风险
- 代码语义更明确
- 更符合高频计数场景的常见实现方式

### 4. 修正一处参数校验错误码不准确的问题

修改文件：

- `src/main/java/com/xzy/forum/services/impl/BoardServiceImpl.java`
- `src/main/java/com/xzy/forum/services/impl/ArticleServiceImpl.java`

修改方式：

- 无效 `boardId` / `articleId` 参数改为返回参数校验错误码，而不是业务统计类错误码

为什么改：

- 参数非法和业务处理失败是两类不同问题
- 错误码不准确会误导前端和排障人员

修改后的好处：

- 错误语义更清晰
- 接口契约更稳定
- 更利于统一异常码治理

### 5. 收紧默认配置的安全基线

修改文件：

- `src/main/java/com/xzy/forum/config/ForumFeatureProperties.java`
- `src/main/java/com/xzy/forum/config/SwaggerConfig.java`
- `src/main/java/com/xzy/forum/controller/TestController.java`
- `src/main/resources/application.yml`

修改方式：

- `test-api-enabled` 默认值改为 `false`
- `TestController` 去掉 `matchIfMissing = true`
- `SwaggerConfig` 去掉 `matchIfMissing = true`
- `FORUM_DB_PASSWORD` 不再带明文默认密码

为什么改：

- 测试接口不应在默认配置缺失时自动暴露
- 安全相关开关更适合“显式开启”，不适合“缺省开启”
- 明文默认密码是公司代码审查中的高频问题

修改后的好处：

- 默认运行基线更安全
- 更接近生产配置思路
- 减少把测试能力误暴露到非测试环境的风险

### 6. 规范回复服务的输入处理和日志

修改文件：

- `src/main/java/com/xzy/forum/services/impl/ArticleReplyServiceImpl.java`

修改方式：

- 回复内容落库前做 `trim`
- 用统一校验工具校验 `articleId`
- 把字符串拼接日志改成占位符日志

为什么改：

- 输入值不规整会带来脏数据
- 手写参数判断和字符串拼接日志可维护性较差

修改后的好处：

- 数据更整洁
- 日志更规范
- 服务层风格更统一

## 验证方式

建议直接执行：

```powershell
.\mvnw.cmd test
```

如果只想先验证本轮最关键改动，也可以重点看：

- `UserControllerAuthIntegrationTest`
- `ArticleServiceImplTest`
- `BoardServiceImplTest`

## 本次整改后的直接收益

- 认证失败行为更稳定，不会把非法 token 混成普通系统异常
- 文章计数逻辑更适合并发场景
- 默认配置更安全，不再默认暴露测试接口和明文密码
- 依赖关系更显式，后续维护和测试成本更低
- 服务层校验和日志风格更统一，更接近团队协作代码

## 后续还可以继续推进的方向

- 给 Controller 层补 DTO，请求参数不要长期散落在大量 `@RequestParam`
- 给核心 Service 补更多异常分支测试和并发场景测试
- 继续清理剩余旧风格类中的注入、注释和命名问题
- 逐步把接口返回对象和数据库实体解耦
