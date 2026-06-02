# Forum System Redis 改造说明

## 1. 这次改造的目标

这次不是单纯“把 Redis 依赖加进来”，而是按更接近正式上线的方式，把项目从本地内存缓存和单机会话，升级成：

- 生产环境默认使用 Redis 作为缓存层
- 生产环境默认使用 Redis 托管登录会话
- 热点数据具备明确的缓存名称、TTL 和失效策略
- 本地开发和测试环境继续可以不依赖 Redis 运行

## 2. 新增了什么

### 2.1 依赖

在 `pom.xml` 中新增：

- `spring-boot-starter-data-redis`
- `spring-boot-starter-session-data-redis`
- `commons-pool2`

作用：

- 让 Spring Cache 可以切换到 Redis
- 让 `HttpSession` 可以落到 Redis，而不是只放在单机 JVM 内存里
- 为 Redis 连接池提供支持

### 2.2 配置类

新增 `src/main/java/com/xzy/forum/config/RedisConfig.java`：

- 自定义 Redis `CacheManager`
- 统一 Redis 序列化方式
- 为不同缓存定义不同 TTL
- 给 Spring Session 提供 Redis 序列化器

### 2.3 生产配置

新增 `src/main/resources/application-prod.yml`，用于正式环境：

- `spring.cache.type=redis`
- `spring.session.store-type=redis`
- `spring.data.redis.*` 连接信息
- Session Cookie 基础安全配置

### 2.4 开发和测试环境降级策略

为了避免你本地开发和自动化测试被 Redis 强依赖卡住，做了降级配置：

- `application-local.yml`：
  - `spring.session.store-type=none`
  - `spring.cache.type=simple`
- `application-test.yml`
  - `spring.session.store-type=none`
  - `spring.cache.type=none`
- `src/test/resources/application-test.yml`
  - `spring.session.store-type=none`
  - `spring.cache.type=none`

这样做的原因是：

- 本地开发先保证能跑
- 单元测试 / 集成测试避免被缓存污染结果
- 生产环境再使用 Redis 提供分布式能力

## 3. Redis 具体用到了哪些地方

### 3.1 用户缓存

`UserServiceImpl`

- `selectById` 使用缓存 `users`
- `updateProfile`、`changePassword` 时主动清理对应用户缓存

目的：

- 用户资料是典型读多写少数据
- 适合放到 Redis 做短 TTL 缓存

### 3.2 版块缓存

`BoardServiceImpl`

- `selectByNum` 使用缓存 `boards`
- `selectAllNormal` 使用缓存 `boards`
- `selectById` 使用缓存 `boards`
- 版块文章数变化时清空相关缓存

目的：

- 首页版块、版块导航、版块详情都属于高频热点
- 正式上线时适合缓存到 Redis

### 3.3 帖子列表缓存

`ArticleServiceImpl`

- `selectAll`
- `selectAllByBoardId`
- `selectAllByUserId`

以上接口统一使用缓存 `articleLists`

目的：

- 首页帖子列表
- 板块帖子列表
- 个人主页发帖列表

这些都是论坛里最典型的热点查询

对应失效场景：

- 发帖
- 编辑帖子
- 删除帖子
- 点赞
- 回复数变化
- 板块文章数变化
- 用户资料变化

这些操作都会清掉 `articleLists`

### 3.4 未读消息数缓存

`MessageServiceImpl`

- `countUnread` 使用缓存 `messageUnreadCounts`
- 发送消息、标记已读、回复消息后会主动清理未读数缓存

目的：

- 首页铃铛未读数属于高频、小结果集、重复读取的热点数据

## 4. 缓存 TTL 设计

在 `RedisConfig` 中做了差异化 TTL：

- `boards`：30 分钟
- `users`：15 分钟
- `articleLists`：5 分钟
- `messageUnreadCounts`：30 秒
- 默认缓存：10 分钟

设计思路：

- 版块变化少，可以更长
- 用户信息中等频率变化
- 帖子列表变化更频繁，TTL 要更短
- 未读数对实时性要求最高，所以 TTL 最短

## 5. 会话为什么要放 Redis

当前项目很多接口还是基于 `HttpSession` 登录态：

- 用户登录后把 `User` 放进 Session
- 文章、回复、私信接口都依赖 Session 判断登录状态

如果正式上线仍使用 JVM 本地 Session，会有几个问题：

- 多实例部署时登录态无法共享
- 某台机器重启后用户会掉线
- 后续接入负载均衡时会话粘滞不稳定

改成 Redis Session 后：

- 多台应用实例共享登录态
- 更接近真实生产环境
- 为后续网关和横向扩容打基础

## 6. 如何在正式环境启用

### 6.1 启动 profile

正式环境建议启用：

```bash
--spring.profiles.active=prod
```

### 6.2 关键环境变量

数据库：

- `FORUM_DB_URL`
- `FORUM_DB_USERNAME`
- `FORUM_DB_PASSWORD`

Redis：

- `FORUM_REDIS_HOST`
- `FORUM_REDIS_PORT`
- `FORUM_REDIS_PASSWORD`
- `FORUM_REDIS_DATABASE`
- `FORUM_REDIS_POOL_MAX_ACTIVE`
- `FORUM_REDIS_POOL_MAX_IDLE`
- `FORUM_REDIS_POOL_MIN_IDLE`
- `FORUM_REDIS_POOL_MAX_WAIT`
- `FORUM_SESSION_COOKIE_SECURE`

### 6.3 示例

```bash
java -jar forum.jar \
  --spring.profiles.active=prod \
  --FORUM_DB_URL=jdbc:mysql://127.0.0.1:3306/forum_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8 \
  --FORUM_DB_USERNAME=root \
  --FORUM_DB_PASSWORD=your-password \
  --FORUM_REDIS_HOST=127.0.0.1 \
  --FORUM_REDIS_PORT=6379
```

## 7. 为什么测试环境关闭缓存

测试环境如果保留缓存，会出现一个典型问题：

- 前一个测试把查询结果缓存住
- 后一个测试虽然事务回滚了数据库，但缓存没清
- 最终导致断言结果和数据库实际状态不一致

所以测试环境统一使用：

- `spring.cache.type=none`
- `spring.session.store-type=none`

这样回归测试更稳定、更可信

## 8. 当前版本的上线建议

这次 Redis 改造已经把“论坛正式上线应该具备的缓存与会话基础”补上了，但如果要继续往生产级推进，建议下一步继续补：

- Redis 监控与慢查询排查
- 缓存击穿 / 穿透保护
- 热点 Key 限流
- 帖子详情阅读量异步累计
- Docker Compose / K8s 部署 Redis
- Redis 高可用（主从 / Sentinel / Cluster）

## 9. 总结

这次改造的核心价值不是“项目里出现了 Redis”，而是：

- Redis 真正接入到了热点数据缓存
- Redis 真正接管了正式环境会话
- 本地开发和测试仍保持低门槛
- 项目结构更接近可上线的单体论坛架构

所以它已经比“只会用 Caffeine 本地缓存”的课程项目更接近真实线上系统。
