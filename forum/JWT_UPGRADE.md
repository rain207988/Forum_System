# Forum JWT 改造说明

## 1. 这次改造的目标

本次把项目原来的 `HttpSession` 登录态，整体升级为 `JWT Token` 认证。

改造后的目标是：

- 后端不再依赖 `session` 保存用户登录状态
- 前端登录成功后保存 `token`
- 后续所有受保护接口统一通过 `Authorization: Bearer <token>` 访问
- 更适合前后端分离、移动端接入、网关转发和正式上线部署
- 保留 Redis，用于生产环境缓存，以及 `token` 黑名单能力

---

## 2. 改造前是什么样

原项目登录流程是：

1. 用户调用 `/user/login`
2. 后端把用户对象放入 `HttpSession`
3. `LoginInterceptor` 从 `session` 中判断是否已登录
4. `ArticleController`、`MessageController`、`UserController` 等控制器从 `HttpSession` 中取当前用户

这种方案在单体 MVC 项目里能跑，但上线后有几个明显问题：

- 后端必须保存登录态，扩容时要处理 session 共享
- 前后端分离时不方便
- 移动端、小程序、多端接入不友好
- 网关、微服务链路下扩展性较弱

---

## 3. 改造后整体方案

现在改成了下面这套流程：

1. 用户登录 `/user/login`
2. 后端校验用户名密码
3. 后端签发 JWT
4. 前端把 JWT 保存到 `localStorage`
5. 前端后续请求自动带上 `Authorization` 请求头
6. 后端拦截器统一校验 JWT
7. 校验通过后，把当前用户放入 `AuthContext`
8. 控制器直接从 `AuthContext` 获取当前登录用户

退出登录时：

1. 前端调用 `/user/logout`
2. 后端把当前 token 加入黑名单
3. 前端删除本地 token

密码修改后：

- 旧 token 会因为密码摘要变化自动失效，必须重新登录

---

## 4. 改了哪些地方

### 4.1 Maven 依赖

文件：

- [pom.xml](D:/code_java/forum/Forum_System/forum/pom.xml)

新增：

- `jjwt-api`
- `jjwt-impl`
- `jjwt-jackson`

移除：

- `spring-boot-starter-session-data-redis`

原因：

- 既然认证已经从 Session 改成 JWT，就不需要 Redis Session 依赖了

---

### 4.2 JWT 核心代码

新增文件：

- [AuthContext.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/auth/AuthContext.java)
- [JwtProperties.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/auth/JwtProperties.java)
- [JwtAuthenticationService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/auth/JwtAuthenticationService.java)
- [TokenRevocationService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/auth/TokenRevocationService.java)
- [AuthResponse.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/dto/AuthResponse.java)

作用分别是：

- `AuthContext`：保存当前请求线程中的登录用户
- `JwtProperties`：读取 JWT 配置
- `JwtAuthenticationService`：负责生成 token、解析 token、校验 token
- `TokenRevocationService`：负责 token 黑名单，优先用 Redis，没 Redis 就本地内存兜底
- `AuthResponse`：登录成功后返回给前端的认证结果对象

---

### 4.3 登录拦截器重写

文件：

- [LoginInterceptor.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/interceptor/LoginInterceptor.java)
- [AppInterceptorConfigurer.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/interceptor/AppInterceptorConfigurer.java)

原来逻辑：

- 从 `HttpSession` 判断是否登录

现在逻辑：

1. 从请求头读取 `Authorization`
2. 提取 `Bearer token`
3. 判断 token 是否在黑名单
4. 解析 token 中的用户 ID
5. 再查数据库拿最新用户
6. 校验 token 是否仍然有效
7. 放入 `AuthContext`

这样做的好处：

- 用户被禁用、密码被修改、资料有变化时，不会长期使用旧登录态
- 后端始终以数据库中的最新用户状态为准

---

### 4.4 控制器不再依赖 HttpSession

重点改动文件：

- [UserController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/UserController.java)
- [MessageController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/MessageController.java)
- [ArticleController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/ArticleController.java)
- [ArticleReplyController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/ArticleReplyController.java)

改造内容：

- 删除 `HttpSession` 参数
- 删除 `session.setAttribute(...)`
- 删除 `session.removeAttribute(...)`
- 删除 `requireLoginUser(HttpSession session)` 这类写法
- 全部改为 `AuthContext.requireCurrentUser()`

这一步是核心，因为这意味着业务层已经完全脱离 Session。

---

### 4.5 登录和退出接口变化

文件：

- [UserController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/UserController.java)

#### 登录接口

原来：

- 登录成功后直接把 `user` 放进 session

现在：

- 登录成功后返回：
  - `user`
  - `token`
  - `tokenType`
  - `expiresAt`

#### 退出接口

原来：

- 清理 session

现在：

- 把当前 token 加入黑名单

---

### 4.6 全局异常处理优化

文件：

- [GlobalExceptionHandler.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/exception/GlobalExceptionHandler.java)

改动：

- 登录失效时返回 `401`
- 参数错误返回 `400`
- 权限问题返回 `403`

这样前端可以更准确地识别“是未登录”还是“参数错误”。

---

### 4.7 前端统一接入 JWT

核心文件：

- [common.js](D:/code_java/forum/Forum_System/forum/src/main/resources/static/js/common.js)
- [sign-in.html](D:/code_java/forum/Forum_System/forum/src/main/resources/static/sign-in.html)
- [index.html](D:/code_java/forum/Forum_System/forum/src/main/resources/static/index.html)
- [settings.html](D:/code_java/forum/Forum_System/forum/src/main/resources/static/settings.html)

主要改动：

#### `common.js`

新增统一认证工具：

- `getForumToken()`
- `storeForumAuth()`
- `clearForumAuth()`
- `redirectToLogin()`
- `initForumAjaxAuth()`

统一行为：

- 非登录/注册页如果没有 token，直接跳到登录页
- 所有 jQuery Ajax 请求自动加 `Authorization`
- 如果后端返回 `401`，前端自动清 token 并跳转登录页

#### `sign-in.html`

登录成功后：

- 把 token 保存到 `localStorage`
- 再跳转到首页

#### `index.html`

退出登录时：

- 先调用后端退出接口
- 再清理本地 token
- 然后跳转登录页

#### `settings.html`

密码修改成功后：

- 清理本地 token
- 强制重新登录

---

### 4.8 配置文件调整

文件：

- [application.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application.yml)
- [application-local.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application-local.yml)
- [application-prod.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application-prod.yml)
- [application-test.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application-test.yml)
- [src/test/resources/application-test.yml](D:/code_java/forum/Forum_System/forum/src/test/resources/application-test.yml)

主要调整：

#### 新增 JWT 配置

在 `application.yml` 中增加：

- `forum.jwt.secret`
- `forum.jwt.expire-hours`
- `forum.jwt.issuer`
- `forum.jwt.header`
- `forum.jwt.prefix`

#### 删除 Session 配置

删除：

- `spring.session.store-type`
- `server.servlet.session.cookie`
- 旧的 session 相关配置

原因：

- 认证体系已经切换为 JWT，不再依赖 Session

#### Redis 现在的作用

现在 Redis 不再用于 Session，而是用于：

- 热点缓存
- token 黑名单

这更符合上线项目里 Redis 的实际使用方式。

---

### 4.9 MySQL 集成测试也改成 JWT

文件：

- [MessageControllerMysqlIntegrationTest.java](D:/code_java/forum/Forum_System/forum/src/test/java/com/xzy/forum/controller/MessageControllerMysqlIntegrationTest.java)

原来：

- 用 `MockHttpSession`

现在：

- 测试里先生成 JWT
- 请求时通过 `Authorization: Bearer <token>` 访问 message 接口

这一步很重要，因为它证明：

- 不是只有页面层改了
- 后端 message 模块在真实 JWT 模式下也能工作

---

## 5. 为什么这样设计

### 5.1 为什么 token 里不直接塞完整用户对象

因为用户资料可能变化：

- 昵称会改
- 头像会改
- 用户状态可能被禁用
- 密码会修改

所以 token 里只保留必要身份信息，服务端每次再查一次数据库拿最新用户，更安全，也更稳。

### 5.2 为什么还保留 Redis

虽然不再用 Redis Session，但 Redis 仍然很有价值：

- 缓存热点数据
- 保存退出登录后的 token 黑名单
- 以后还可以做限流、验证码、热点帖子缓存、排行榜等

### 5.3 为什么退出登录要做 token 黑名单

JWT 默认是无状态的，签发后在有效期内都能用。  
如果不做黑名单：

- 用户点“退出登录”后，旧 token 理论上还能继续访问

加入黑名单后：

- 退出就能立即失效

---

## 6. 现在如何使用

### 6.1 登录

请求：

`POST /user/login`

登录成功后返回：

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "user": {},
    "token": "xxxxx",
    "tokenType": "Bearer",
    "expiresAt": "2026-06-02T..."
  }
}
```

前端需要保存：

- `data.token`
- `data.tokenType`

---

### 6.2 携带 token 访问接口

请求头格式：

```http
Authorization: Bearer your-jwt-token
```

浏览器端现在已经在 `common.js` 自动处理，不需要每个页面手写。

---

### 6.3 退出登录

请求：

`GET /user/logout`

前端会：

1. 调用后端
2. 清理本地 token
3. 跳回登录页

---

## 7. 本地开发和测试方式

### 7.1 默认测试

```powershell
.\mvnw.cmd test
```

结果：

- 已通过

### 7.2 本地 MySQL 验证 message 模块

PowerShell 下正确写法：

```powershell
.\mvnw.cmd test "-Dforum.mysql.it=true"
```

结果：

- 已通过

这一步验证了：

- JWT 登录链路可用
- 本地 MySQL 下 `message` 模块可用
- `send / unread count / inbox / markRead / reply` 都正常

---

## 8. 这次改造后，项目的提升

这次不是简单“把一个 API 改了”，而是把认证架构升级了：

- 从传统 MVC Session 认证，升级为 JWT 认证
- 认证方式更接近正式上线项目
- 更适合前后端分离和多端接入
- Redis 的使用场景也更接近真实生产实践
- message 模块已经完成 JWT + MySQL 联调验证

---

## 9. 后续还可以继续升级

后续如果继续往正式上线方向推进，建议下一步做这些：

1. 引入刷新 token 机制，区分 access token 和 refresh token
2. 给 JWT 加更细的权限字段，比如管理员权限
3. 增加登录失败次数限制和验证码
4. Redis 增加限流、热点帖子缓存、未读消息计数优化
5. 站内信升级为 WebSocket 实时推送
6. 前端把静态页面逐步升级成 Vue/React 前后端分离模式

---

## 10. 本次结论

现在这个论坛项目已经从：

- `Session 登录态`

升级为：

- `JWT Token 认证`

并且已经完成：

- 默认测试通过
- 本地 MySQL message 集成测试通过
- 前端页面接入 token
- 后端控制器和拦截器完全去 session 化
- 改造说明文档落库

如果后面你还要继续升级，我建议下一步做：

- `refresh token`
- `WebSocket 实时消息`
- `权限体系`
- `前后端分离`
