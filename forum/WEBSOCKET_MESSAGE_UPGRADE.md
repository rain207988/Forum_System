# Forum 站内信 WebSocket 实时升级说明

## 1. 这轮升级做了什么

这次把项目里的站内信从“普通接口拉取”升级成了“真正的 WebSocket 实时通知”。

保留了你现有的：

- `message` 模块接口
- 首页右上角小铃铛
- 右侧站内信侧边栏
- 原有的发送、回复、已读弹窗结构

新增的是一条实时链路：

- 新站内信到达时，浏览器会立刻收到推送
- 未读红点会自动刷新
- 收件箱列表会自动刷新
- 已读/回复后，本页消息区域也能同步刷新

---

## 2. 升级前的问题

升级前站内信虽然能用，但本质上还是“同步接口 + 手动刷新”：

- 页面初始化时调用 `/message/getUnreadCount`
- 页面初始化时调用 `/message/getAll`
- 发送/回复/已读后，再手动重新请求一次

这意味着：

- 别人给你发消息时，你当前页面不会立即知道
- 小红点不是实时的
- 消息列表也不是实时的

这更像“站内信功能可用”，还不是“即时提醒”。

---

## 3. 这轮改了哪些后端内容

### 3.1 引入 WebSocket 能力

修改文件：

- [pom.xml](D:/code_java/forum/Forum_System/forum/pom.xml)

新增依赖：

- `spring-boot-starter-websocket`

作用：

- 给项目补齐原生 WebSocket 支持

---

### 3.2 新增 WebSocket 配置与握手鉴权

新增文件：

- [src/main/java/com/xzy/forum/config/ForumMessageWebSocketConfig.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/ForumMessageWebSocketConfig.java)
- [src/main/java/com/xzy/forum/websocket/AuthenticatedUserHandshakeInterceptor.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/websocket/AuthenticatedUserHandshakeInterceptor.java)
- [src/main/java/com/xzy/forum/websocket/ForumMessageWebSocketHandler.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/websocket/ForumMessageWebSocketHandler.java)
- [src/main/java/com/xzy/forum/websocket/MessageWebSocketSessionRegistry.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/websocket/MessageWebSocketSessionRegistry.java)

实现方式：

- WebSocket 入口地址为 `/ws/messages`
- 握手时通过 `token` 参数校验当前 JWT
- 校验通过后，把当前连接和用户 `userId` 绑定
- 一个用户可以有多个会话，支持多标签页同时在线

为什么这样做：

- 不额外引入 STOMP，改动更轻
- 直接复用现有 JWT 认证体系
- 不破坏当前 Controller/Service 结构

---

### 3.3 新增站内信实时通知服务

新增文件：

- [src/main/java/com/xzy/forum/websocket/MessageRealtimeNotificationService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/websocket/MessageRealtimeNotificationService.java)

作用：

- 给指定用户推送 WebSocket 事件
- 支持两类事件：
  - `message-created`
  - `message-inbox-refresh`

重点设计：

- 推送在事务提交后触发

为什么一定要事务提交后推送：

- 如果数据库事务还没提交，前端先收到事件再去查接口，可能查不到刚写入的数据
- 放到 `afterCommit` 后，前端刷新时能拿到稳定数据

这一步很关键，它决定了实时提醒是不是“看起来实时但偶尔不一致”。

---

### 3.4 把 MessageService 接上实时推送链路

修改文件：

- [src/main/java/com/xzy/forum/services/impl/MessageServiceImpl.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/impl/MessageServiceImpl.java)

接入点：

- `send(...)`
  - 给接收者推送 `message-created`
- `reply(...)`
  - 原消息接收者页面刷新自己的收件箱状态
  - 回复消息接收方通过 `send(...)` 自动收到新消息事件
- `markRead(...)`
  - 给当前用户推送 `message-inbox-refresh`

这样处理后：

- 收到新消息时，接收者立即更新
- 已读、回复这些动作也会让当前页面同步刷新

---

## 4. 这轮改了哪些前端内容

### 4.1 在公共脚本中新增 WebSocket 连接能力

修改文件：

- [src/main/resources/static/js/common.js](D:/code_java/forum/Forum_System/forum/src/main/resources/static/js/common.js)

新增能力：

- 建立 `/ws/messages` WebSocket 连接
- 自动携带当前 access token
- 连接断开后自动重连
- access token 刷新后自动重连 WebSocket
- 提供统一实时事件分发方法

关键方法：

- `connectForumMessageSocket()`
- `closeForumMessageSocket()`
- `onForumRealtimeEvent(handler)`

这样做的好处：

- 后续如果别的页面也想接实时站内信，不需要再重复造连接代码
- token 刷新后，长连接也能跟着切到新 token

---

### 4.2 首页消息区域接入实时刷新

修改文件：

- [src/main/resources/static/index.html](D:/code_java/forum/Forum_System/forum/src/main/resources/static/index.html)

这次没有重做你的消息 UI，而是在现有结构上做增强：

- 监听 WebSocket 事件
- 收到 `message-created` 或 `message-inbox-refresh` 时：
  - 重新请求未读数
  - 重新请求消息列表
- 收到新消息时弹一个轻量提示

保留了你现有的：

- 小铃铛红点
- 站内信侧边栏
- 查看/回复模态框
- `requestMessageUnreadCount()`
- `requestMessageList()`

也就是说这轮核心不是“重写前端消息页”，而是“用 WebSocket 驱动现有页面自动刷新”。

---

## 5. 安全与上线相关补充

### 5.1 WebSocket 继续复用 JWT 登录态

没有回退到 Session。

握手时仍然走 token 校验，保持和当前项目统一：

- 普通接口：JWT
- WebSocket 握手：JWT

这样避免系统里出现两套认证模型。

---

### 5.2 默认 CSP 放通 `ws:` / `wss:`

修改文件：

- [src/main/resources/application.yml](D:/code_java/forum/Forum_System/forum/src/main/resources/application.yml)
- [src/main/java/com/xzy/forum/config/ForumSecurityHeadersProperties.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/config/ForumSecurityHeadersProperties.java)

原因：

- 如果 CSP 的 `connect-src` 不允许 WebSocket，浏览器会直接拦掉连接

现在默认值已经允许：

- `connect-src 'self' ws: wss:`

---

## 6. 这轮升级后的实际效果

现在项目里的站内信行为已经从：

- 打开页面拉一次
- 发送/回复后自己手动再拉一次

升级成：

- 新消息到达时自动推送
- 未读红点自动变化
- 收件箱列表自动刷新
- 当前页面已读/回复后同步刷新

这比原来更像真实产品里的“即时站内信提醒”。

---

## 7. 自动化验证

新增测试文件：

- [src/test/java/com/xzy/forum/websocket/ForumMessageWebSocketIntegrationTest.java](D:/code_java/forum/Forum_System/forum/src/test/java/com/xzy/forum/websocket/ForumMessageWebSocketIntegrationTest.java)

验证点：

- 用有效 JWT 建立 WebSocket 连接
- 调用 `messageService.send(...)`
- 断言客户端能收到 `message-created` 实时事件

这能证明：

- 握手鉴权有效
- 推送链路有效
- 事务提交后的通知有效

---

## 8. 这轮建议如何验证

### 8.1 默认测试

```powershell
.\mvnw.cmd test
```

### 8.2 本地 MySQL 站内信链路

```powershell
.\mvnw.cmd test "-Dforum.mysql.it=true"
```

### 8.3 手动体验

建议开两个浏览器登录两个账号：

1. 账号 A 打开首页
2. 账号 B 给账号 A 发送站内信
3. 观察账号 A：
   - 右上角红点是否自动出现
   - 消息侧边栏是否自动更新
   - 是否出现实时提示

---

## 9. 后续还可以继续升级什么

如果继续往更像正式上线的标准推进，下一步可以考虑：

1. 把实时事件从“刷新整个列表”进一步升级成“局部增量更新”
2. 给站内信增加在线状态、最近联系人列表
3. 增加消息删除/撤回能力
4. 给 WebSocket 增加 Redis Pub/Sub 支撑多实例部署
5. 给实时通道补监控和连接数指标

目前这轮已经把“实时提醒”这一关键步补上了。
