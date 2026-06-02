# Forum 代码规范整改说明

## 1. 这轮整改做了什么

这次不是继续堆功能，而是对项目主代码做了一轮偏“公司代码审查标准”的规范整改，重点处理了下面几类问题：

- 统一把核心控制器、服务类从字段注入改成构造器注入
- 修复一批命名不规范的方法名
- 去掉 Redis 降级链路里的静默吞异常
- 清理前端公共脚本中的低质量调试代码和可读性问题

这类改动表面上不像新功能那么显眼，但对后续维护、测试、排障和团队协作都很重要。

---

## 2. 为什么这些地方不符合公司级别代码规范

在真实团队里，这几类写法通常都会被 code review 卡住：

### 2.1 字段注入 `@Autowired`

字段注入的问题主要有：

- 依赖关系不够显式，类一打开看不出完整依赖
- 成员变量不能自然声明为 `final`
- 单元测试或构造对象时不够方便
- 更容易出现隐藏依赖越来越多的问题

所以更常见、更规范的做法是：

- 用构造器注入
- 把依赖声明成 `final`

---

### 2.2 命名不规范

例如原来存在：

- `createnormalUser`
- `addOneArticleCountById`
- `subOneArticleCountById`

这些命名的问题是：

- 不符合标准驼峰命名
- 动作语义不统一
- `addOne` / `subOne` 更像口语，不像团队内长期维护的接口命名

正式项目里，更推荐：

- `createNormalUser`
- `incrementArticleCountById`
- `decrementArticleCountById`

这样一眼就能看懂行为，而且接口命名风格统一。

---

### 2.3 静默吞异常

原来 Redis 相关逻辑里有多处：

- `catch (Exception ignored)`

这种写法最大的问题不是“会不会崩”，而是：

- 线上一旦 Redis 出故障，日志里没有任何有效信息
- 代码虽然降级成功了，但运维和开发并不知道发生过降级
- 后面排查登录态、限流、token 黑名单问题会非常痛苦

公司项目里，允许降级，但不应该无声失败。

---

### 2.4 前端公共脚本里保留调试代码

`common.js` 里有一些明显的调试输出和布尔写法问题，例如：

- `if (xxx == false)`
- 多余的 `console.log`

这类代码在开发阶段无所谓，但进入主干后会降低公共脚本质量，也容易让后续排查日志时出现噪音。

---

## 3. 具体修改了哪里

### 3.1 构造器注入整改

修改文件：

- [src/main/java/com/xzy/forum/controller/UserController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/UserController.java)
- [src/main/java/com/xzy/forum/controller/BoardController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/BoardController.java)
- [src/main/java/com/xzy/forum/controller/ArticleController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/ArticleController.java)
- [src/main/java/com/xzy/forum/controller/MessageController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/MessageController.java)
- [src/main/java/com/xzy/forum/interceptor/AppInterceptorConfigurer.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/interceptor/AppInterceptorConfigurer.java)
- [src/main/java/com/xzy/forum/services/impl/UserServiceImpl.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/impl/UserServiceImpl.java)
- [src/main/java/com/xzy/forum/services/impl/BoardServiceImpl.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/impl/BoardServiceImpl.java)
- [src/main/java/com/xzy/forum/services/impl/ArticleServiceImpl.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/impl/ArticleServiceImpl.java)
- [src/main/java/com/xzy/forum/services/impl/MessageServiceImpl.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/impl/MessageServiceImpl.java)

这部分修改内容：

- 移除字段上的 `@Autowired`
- 依赖改成 `private final`
- 统一补上构造器注入
- 对可选依赖 `CacheManager` 使用 `ObjectProvider` 获取，避免强依赖

整改后的好处：

- 依赖更明确
- 类的可维护性更好
- 更方便做单测和重构
- 更接近主流 Spring 团队规范

---

### 3.2 服务层方法命名整改

修改文件：

- [src/main/java/com/xzy/forum/services/IUserService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/IUserService.java)
- [src/main/java/com/xzy/forum/services/IBoardService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/IBoardService.java)
- [src/main/java/com/xzy/forum/services/impl/UserServiceImpl.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/impl/UserServiceImpl.java)
- [src/main/java/com/xzy/forum/services/impl/BoardServiceImpl.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/impl/BoardServiceImpl.java)
- [src/main/java/com/xzy/forum/services/impl/ArticleServiceImpl.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/services/impl/ArticleServiceImpl.java)
- [src/main/java/com/xzy/forum/controller/UserController.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/controller/UserController.java)
- [src/test/java/com/xzy/forum/services/impl/BoardServiceImplTest.java](D:/code_java/forum/Forum_System/forum/src/test/java/com/xzy/forum/services/impl/BoardServiceImplTest.java)

重命名如下：

- `createnormalUser` -> `createNormalUser`
- `addOneArticleCountById` -> `incrementArticleCountById`
- `subOneArticleCountById` -> `decrementArticleCountById`
- `addOneArticleCount` -> `incrementArticleCountById`

整改后的好处：

- 命名语义统一
- 接口更容易理解
- 降低新成员接手时的理解成本
- 后续继续扩展统计字段时命名风格更稳定

---

### 3.3 Redis 降级异常处理整改

修改文件：

- [src/main/java/com/xzy/forum/service/RateLimitService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/service/RateLimitService.java)
- [src/main/java/com/xzy/forum/auth/TokenRevocationService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/auth/TokenRevocationService.java)
- [src/main/java/com/xzy/forum/auth/RefreshTokenSessionService.java](D:/code_java/forum/Forum_System/forum/src/main/java/com/xzy/forum/auth/RefreshTokenSessionService.java)

这部分改动：

- 给类补上日志能力
- 把 `catch (Exception ignored)` 改成带上下文的 `warn` 日志
- 仍然保留本地内存降级逻辑，不影响本地开发和服务可用性

整改后的好处：

- Redis 故障时可以快速定位问题
- 能区分“正常走 Redis”还是“已经降级到本地内存”
- 更符合线上可观测性要求

---

### 3.4 前端公共脚本清理

修改文件：

- [src/main/resources/static/js/common.js](D:/code_java/forum/Forum_System/forum/src/main/resources/static/js/common.js)

这部分改动：

- `if (boardItem.hasClass('active') == false)` 改成 `if (!boardItem.hasClass('active'))`
- 删除帖子列表和站内信接收信息相关的临时 `console.log`

整改后的好处：

- 公共脚本更干净
- 可读性更好
- 线上浏览器控制台噪音更少

---

## 4. 这轮整改后项目有哪些直接收益

这轮改完之后，项目虽然业务功能没变多，但代码质量明显更像可长期维护的项目：

- 依赖关系更加显式
- 核心服务命名更加统一
- Redis 降级链路具备基本可观测性
- 公共前端脚本更加整洁
- 后续继续做重构、补单测、拆模块时成本更低

从“个人项目能跑”往“团队项目能接手、能维护、能排障”又往前走了一步。

---

## 5. 这轮整改如何验证

建议验证方式：

### 5.1 默认测试

```powershell
.\mvnw.cmd test
```

### 5.2 本地 MySQL 的 message 链路测试

```powershell
.\mvnw.cmd test "-Dforum.mysql.it=true"
```

如果这两组都通过，说明这轮规范整改没有破坏默认开发链路，也没有破坏你之前要求保留的本地 MySQL 站内信验证链路。

---

## 6. 下一轮还可以继续补哪些“公司级规范”

如果继续按公司级别代码标准推进，下一轮建议优先看：

1. 统一 Controller 层参数对象，减少大量 `@RequestParam`
2. 补充更系统的 DTO / VO 分层，避免直接暴露数据库模型
3. 给核心 Service 增加更完整的单元测试和边界场景测试
4. 统一异常码、日志字段、审计日志格式
5. 对前端公共 API 请求层做更清晰的封装，减少页面脚本直连接口

这些会比单纯“修几个命名”更进一步，把项目往真正的团队工程质量继续推进。
