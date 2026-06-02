# Forum Refresh Token 升级说明

## 1. 这轮升级做了什么

这次是在上一轮 `JWT Token` 登录改造的基础上，继续把认证体系往正式上线项目的标准推进了一步：

- 增加了 `access token + refresh token` 双令牌机制
- 新增了 refresh token 轮换能力
- 前端增加了无感刷新和 401 自动重试
- 退出登录、修改密码时，除了让 access token 失效，也会让 refresh token 会话失效
- 增加了自动化测试，验证刷新、轮换、退出后失效、改密后失效这些关键场景

这轮改造的重点不是“再多一个接口”，而是把登录态的完整生命周期补齐。

---

## 2. 改造前存在的问题

上一轮 JWT 改造后，项目已经不再依赖 Session，基本可用。

但仍然有一个典型问题：

- 前端只保存了一个 access token
- 一旦 token 过期，页面会直接跳回登录页
- 站内信、个人中心、帖子列表这些页面请求一旦返回 `401`，用户体验会比较生硬

这在开发阶段能接受，但离正式上线常见方案还差一步。

---

## 3. 这轮改造后的整体方案

现在的认证模型变成了：

1. 登录成功后，后端同时签发：
   - `access token`
   - `refresh token`
2. `access token` 用于正常访问受保护接口
3. `refresh token` 用于在 access token 过期前后，向 `/user/refreshToken` 申请新 token
4. 每次刷新时：
   - 重新签发新的 access token
   - 同时轮换新的 refresh token
5. 退出登录、修改密码后：
   - access token 失效
   - refresh token 对应会话也失效

这样做的结果是：

- 用户不用频繁重新登录
- 前端能在 token 快过期时提前刷新
- refresh token 不是无限可用，而是有服务端状态控制和轮换约束

---

## 4. 后端改了哪些地方

### 4.1 JWT 配置增强

文件：

- [src/main/java/com/xzy/forum/auth/JwtProperties.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/auth/JwtProperties.java)
- [src/main/resources/application.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application.yml)

新增配置：

- `forum.jwt.refresh-expire-days`

同时把 access token 默认有效期从 12 小时调整为 2 小时，refresh token 默认 7 天。

这样更贴近生产做法：

- access token 更短，泄露风险更可控
- refresh token 更长，用户体验更稳定

---

### 4.2 JWT 服务增强

文件：

- [src/main/java/com/xzy/forum/auth/JwtAuthenticationService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/auth/JwtAuthenticationService.java)

这次主要补了下面这些能力：

- 区分 token 类型：
  - `access`
  - `refresh`
- 在 refresh token 中保存 `refreshSessionId`
- 新增 refresh token 生成与轮换方法
- 新增 refresh token 专用校验方法
- 给每个 JWT 增加唯一 `jti`

关键点：

1. access token 和 refresh token 不再混用
2. refresh token 不能直接拿去访问业务接口
3. 同一秒内重新签发 access token 时，也能保证 token 内容不同

---

### 4.3 新增 refresh token 会话存储服务

文件：

- [src/main/java/com/xzy/forum/auth/RefreshTokenSessionService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/auth/RefreshTokenSessionService.java)

这是这轮后端的核心新增类。

作用：

- 保存 refresh token 的服务端会话状态
- 校验某个 refresh token 是否仍然是当前有效版本
- 退出登录或改密时删除该 refresh 会话

实现方式：

- 优先使用 Redis
- 如果 Redis 不可用，则退化到本地内存 `ConcurrentHashMap`

为什么这么设计：

- 本地开发不强制依赖 Redis 也能跑
- 正式上线时又能无缝切到 Redis
- 轮换后的旧 refresh token 可以立刻失效

---

### 4.4 登录、刷新、退出、改密接口联动升级

文件：

- [src/main/java/com/xzy/forum/controller/UserController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/UserController.java)
- [src/main/java/com/xzy/forum/dto/AuthResponse.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/dto/AuthResponse.java)

#### 登录接口 `/user/login`

现在返回字段变成：

- `user`
- `token`
- `tokenType`
- `expiresAt`
- `refreshToken`
- `refreshExpiresAt`

登录成功后，后端会：

1. 生成 access token
2. 生成 refresh token
3. 把 refresh token 的哈希状态写入 Redis 或本地内存

#### 新增刷新接口 `/user/refreshToken`

作用：

- 前端使用 refresh token 换取新的 token 对

刷新成功后：

- 返回新的 access token
- 返回新的 refresh token
- 旧 refresh token 立即失效

#### 退出接口 `/user/logout`

现在除了拉黑 access token 外，还会：

- 根据前端传入的 refresh token 删除对应 refresh 会话

#### 修改密码 `/user/modifyPwd`

现在除了密码改动会导致 tokenVersion 变化外，还会：

- 主动撤销当前 refresh token 会话

这意味着用户改密之后，旧 refresh token 也不能再拿来续期。

---

### 4.5 登录拦截白名单补充

文件：

- [src/main/java/com/xzy/forum/interceptor/AppInterceptorConfigurer.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/interceptor/AppInterceptorConfigurer.java)

新增白名单：

- `/user/refreshToken`

原因很直接：

- refresh 接口本来就是在 access token 失效或即将失效时使用
- 它不能再要求先通过 access token 的登录拦截

---

## 5. 前端改了哪些地方

### 5.1 统一认证层升级

文件：

- [src/main/resources/static/js/common.js](D:/code_java/forum/Forum_System/forum/src/main/resources/static/js/common.js)

这次前端最核心的改动都集中在这里。

新增能力：

- 保存 refresh token
- 保存 access token / refresh token 过期时间
- 在 access token 快过期时预刷新
- 请求返回 `401` 时先尝试 refresh
- refresh 成功后自动重试原请求
- 避免并发请求同时触发多次 refresh

新增的关键方法包括：

- `getForumRefreshToken()`
- `buildForumRefreshTokenPayload()`
- `getForumAccessTokenExpiresAt()`
- `getForumRefreshTokenExpiresAt()`
- `refreshForumAuth()`
- `shouldRefreshAccessTokenSoon()`

这次没有去逐页重写所有请求，而是直接增强了统一的 `$.ajax` 包装层。

这样做的好处：

- 项目里原有的页面代码改动更少
- `index.html`、`settings.html`、`article.html`、`profile.html` 等页面都自动继承新认证能力

---

### 5.2 登录页

文件：

- [src/main/resources/static/sign-in.html](D:/code_java/forum/Forum_System/forum/src/main/resources/static/sign-in.html)

登录成功后现在会一起保存：

- access token
- refresh token
- token 类型
- access token 过期时间
- refresh token 过期时间

---

### 5.3 首页退出登录

文件：

- [src/main/resources/static/index.html](D:/code_java/forum/Forum_System/forum/src/main/resources/static/index.html)

退出时现在会额外带上：

- `refreshToken`

这样后端可以同步删除 refresh 会话，而不只是拉黑当前 access token。

---

### 5.4 用户中心改密

文件：

- [src/main/resources/static/settings.html](D:/code_java/forum/Forum_System/forum/src/main/resources/static/settings.html)

修改密码时现在也会带上：

- `refreshToken`

后端收到后会主动撤销这条 refresh 会话。

---

## 6. 这轮为什么更接近正式上线

这轮升级后的认证方案，更接近真实项目常见做法：

- access token 生命周期更短
- refresh token 生命周期更长
- refresh token 有服务端会话状态
- refresh token 每次刷新都会轮换
- 旧 refresh token 不能重复使用
- 退出登录和改密都能让 refresh 会话失效

这比“只有一个 JWT，过期就直接重新登录”更完整，也更像真正上线系统。

---

## 7. 自动化测试做了什么

新增测试文件：

- [src/test/java/com/xzy/forum/controller/UserControllerAuthIntegrationTest.java](D:/code_java/forum/Forum_System/forum/src/test/java/com/xzy/forum/controller/UserControllerAuthIntegrationTest.java)

这组测试没有依赖固定种子账号密码，而是测试内动态注册新用户，再走完整登录链路。

覆盖了四个核心场景：

1. 登录成功后同时返回 access token 和 refresh token
2. refresh 成功后：
   - 新 access token 可用
   - 新 refresh token 可用
   - 旧 refresh token 失效
3. 退出登录后，旧 refresh token 失效
4. 修改密码后，旧 refresh token 失效

---

## 8. 这轮如何验证

### 8.1 默认测试

执行命令：

```powershell
.\mvnw.cmd test
```

结果：

- 已通过

### 8.2 本地 MySQL 集成测试

执行命令：

```powershell
.\mvnw.cmd test "-Dforum.mysql.it=true"
```

结果：

- 已通过

说明：

- refresh token 升级没有破坏 message 模块的本地 MySQL 真实链路
- 站内信的发送、查询、已读、回复依然可用

---

## 9. 这轮改造后的效果

现在项目的认证体验已经从：

- token 过期就强制跳登录

升级为：

- token 快过期时前端自动刷新
- access token 失效时优先尝试无感续期
- refresh token 轮换，旧 token 立即失效
- 退出和改密都能彻底结束会话

这一步对“更像正式可上线项目”帮助很大。

---

## 10. 下一轮还可以继续做什么

如果继续往上线标准推进，建议下一轮优先考虑：

1. 把 refresh token 从 `localStorage` 再升级到更安全的 `HttpOnly Cookie` 方案
2. 增加登录设备管理和会话列表
3. 接入 WebSocket，把站内信做成真正实时通知
4. 对 refresh token 和登录行为补充审计日志
5. Redis 缓存增加统计能力和命中率监控
6. 补更完整的生产配置，比如 HTTPS、反向代理、日志归档、监控告警

---

## 11. 本轮结论

这轮已经把论坛项目的认证体系从“单 token 可用”升级成了“更完整的双 token 生产化方案”。

已经完成：

- refresh token 后端能力
- refresh token 轮换
- 前端无感刷新
- 401 自动重试
- 登出/改密后的 refresh 会话失效
- 默认测试通过
- 本地 MySQL message 集成测试通过
- 说明文档补齐

这轮完成后，项目在认证这一块已经明显更接近正式上线标准。
