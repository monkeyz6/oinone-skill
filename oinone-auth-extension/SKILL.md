---
name: oinone-auth-extension
description: Oinone/Pamirs RBAC 权限系统扩展。涵盖白名单（fun-filter / fun-filter-only-login）、AuthFilterService 自定义鉴权、数据权限（行级 RSQL 过滤）、字段权限、权限树扩展（PermissionNodeLoadExtendApi）、SSO 对接（UserCookieLogin）、登录流程扩展（PamirsUserTransientExtPoint）、密码与登录规则（UserPatternCheckApi）、角色管理钩子、授权/缓存扩展、RSQL 占位符、Session 初始化、以及运行时权限 API（DataPermissionApi/AccessPermissionApi 等）替换。Use when 用户提到权限白名单、免登录、跳过鉴权、数据权限、行权限、字段权限、登录扩展、SSO 对接、强制改密、密码策略、角色生命周期钩子、动态角色、权限树节点、授权同步外部系统、缓存刷新扩展、@SPI.Service 权限相关 SPI 实现、或需要在 Oinone/Pamirs 框架内自定义任何 RBAC 鉴权链路时。
license: Proprietary
compatibility: Oinone/Pamirs 5.3.x（deli-aries 5.3.9.4 / kailas-aries 5.3.6.2），JDK 8 语法
metadata:
  framework: oinone-pamirs
  category: auth-rbac
---

# Oinone/Pamirs 权限扩展

## 何时启用

需求落在以下任一场景：

- 配置接口免登录 / 仅需登录的白名单
- 自定义鉴权规则（按模块/菜单/动作/函数/模型/字段/行级粒度）
- 数据权限：行级 RSQL 过滤、读/写/删差异化、按部门/创建人/租户隔离
- 自定义 RSQL 占位符或 Session 初始化
- 权限树节点增删改、节点显示文案自定义
- 登录与 SSO：对接第三方 SSO、强制改密、登录后跳转
- 密码策略 / 用户名 / 邮箱 / 手机号校验规则
- 角色生命周期钩子（删除/启用/禁用前后）、动态角色获取
- 授权变更时同步外部系统、自定义缓存刷新
- 替换 `DataPermissionApi` / `AccessPermissionApi` 等运行时核心 API

## 不做什么

明确不在本 skill 覆盖范围内的情况，遇到时切走或停下：

| 场景 | 应对 |
|---|---|
| 创建普通业务模型（@Model / @Field） | 切 `oinone-create-model` |
| 创建 Action / Function / 按钮 | 切 `oinone-create-action` |
| 定时任务 / 异步任务 / 触发任务 | 切对应 `oinone-create-schedule-task` / `oinone-create-async-task` / `oinone-create-trigger-task` |
| 改设计器 UI 上的字段联动 / 弹窗 / 列表列 | 不是代码层的事，去设计器 DB 改（grep 不到 ≠ 未实现） |
| Pamirs 版本 ≠ 5.3.x 或非 JDK 8 | **停下提示用户** — 本 skill 假设的 SPI 签名/Hook 顺序可能不一致 |
| SPI 实现已写但 Pamirs 扫不到 | 先查 `@Component`+`@SPI.Service` 是否同类共置 + 模块是否在启动类 `scanPackages` 内，不是来本 skill 加扩展点 |
| 客户端直接传 token / userId 就让登录 | 拒绝实现并明确告知用户这是"无鉴权" |
| 数据权限想加在 `PROXY` 代理模型上 | 不可行，把数据权限挂到对应真实持久化模型 |

## 验收标准

每次任务完成前逐条 ✅ 勾选，缺一即不算交付：

**通用（任何扩展都需）**
- [ ] SPI 入口类同时挂 `@Component` 和 `@SPI.Service`（同类共置）；ExtPoint 入口类同时挂 `@Ext` 和 `@ExtPoint.Implement`
- [ ] 编译通过：`mvn -s ~/.m2/settings-changsha.xml -Pshushi -pl <module> -am install -DskipTests`
- [ ] JDK 8 语法：无 `var`、文本块、`switch` 表达式、`record`
- [ ] 异常用 `PamirsException` + `ExpEnumerate` 枚举码，禁止 `appendMsg(...)` 拼字符串

**A. 白名单类**
- [ ] YAML 写 `pamirs.auth.fun-filter`（免登录）或 `fun-filter-only-login`（仅需登录），README 区分清楚
- [ ] 代码方式：实现 `AuthFilterService.isAccessXxx`，**未匹配分支返回 `null`，不返回 `false`**
- [ ] `namespace` 用模型编码（如 `demo.DemoModel`，非 Java 包名），`fun` 用函数编码（非 Java 方法名）

**B. 数据权限类**
- [ ] `fetchModelFilterForRead` / `Write` / `Delete` 三方法**同时实现**（缺一即漏洞）
- [ ] 命中：`AuthResult.success("rsql")`；不限制：`AuthResult.success(null)`；不属当前模型/角色：`null`（交链）
- [ ] 用 model 参数（或 `getModel()`）限定目标模型，未匹配返回 `null`
- [ ] 处理 userId 为空 / 角色为空 / 未登录的兜底

**C. 登录 / SSO 类**
- [ ] `UserCookieLogin` 实现带 `@Order(0)`（除非显式替换其他位置并说明理由）
- [ ] **token 必须服务端验证**（调 SSO `userinfo` 等接口），禁止信任客户端 token / userId
- [ ] 写 Session 用 `setUserInfoToCookiesAndSetUserIdToCache(userId, request, response)` 基类方法或 `PamirsUserSession.setUser(user)`，**禁止手搓 cookie**
- [ ] 非本登录方式（如 `loginType` header 不匹配）返回 `null` 把控制权交回默认链

**D. 高级扩展类**
- [ ] 替换运行时 API（如 `DataPermissionApi`）前，列出全部使用方并自检覆盖，不只是自己的模型
- [ ] RSQL 占位符值用枚举/白名单，**禁止拼接用户输入**（防 SQL 注入）

## 架构速览

```
Hook 拦截层（RoleHook → FunctionPermissionHook → DataPermissionHook → FieldPermissionHook）
  ↓
SPI 过滤层（AuthFilterService 链，多实现按 @Order 协作）
  ↓
运行时权限 API 层（DataPermissionApi / AccessPermissionApi / ManagementPermissionApi / VerificationPermissionApi）
  ↓
缓存层（AccessPermissionCacheApi / DataPermissionCacheApi / ...）
```

资源权限分四类：资源权限（模块/菜单/动作/函数）、模型权限、字段权限、行权限（数据权限）。

> Hook 链顺序与每层职责详见 [references/reference.md#hook-执行链](./references/reference.md#hook-执行链)。

## 不确定时必须提问

需求模糊、存在多种方案、影响范围不清时，**必须用 `AskUserQuestion` 工具**确认，禁止纯文本提问，禁止自行假设。苏格拉底式递进，每次 1–5 个问题，方案优劣作为选项描述。

必须确认的三类决策点：

1. **扩展类型** — 白名单 / 自定义鉴权 / 数据权限 / 登录 SSO / 角色钩子 / 高级 API 替换？
2. **影响范围** — 全局还是特定模型/模块？是否区分读/写/删？是否多租户/部门隔离？
3. **技术方案** — 存在多种实现路径时（如 YAML vs `AuthFilterService`），列利弊让用户选。

## 工作流

确认扩展类型（必要时 AskUserQuestion）→ 决策树选方案 → 按锚点加载 examples.md 模板 → 实现并注册 → 用「验收标准」逐条自检。

## 决策树

### 第一轮：确认扩展类型

通过 `AskUserQuestion` 提问，4 选 1：

- **A.** 白名单 / 免鉴权（接口免登录或免权限校验）
- **B.** 数据权限（行级过滤、按部门/创建人筛选）
- **C.** 登录 / SSO / 用户扩展（自定义登录、密码规则、强制改密）
- **D.** 高级扩展（权限树节点、授权扩展、缓存扩展、运行时 API 替换）

### A → 白名单方案

| 子方案 | 场景 | 模板锚点 |
|---|---|---|
| A1. YAML `fun-filter` | 静态、最简单，免登录 | [examples.md#yaml-whitelist](./references/examples.md#yaml-whitelist) |
| A2. YAML `fun-filter-only-login` | 仅需登录不校验权限 | [examples.md#check-login](./references/examples.md#check-login) |
| A3. `AuthFilterService` 代码方式 | 动态判断 / 复杂条件 | [examples.md#auth-filter-service](./references/examples.md#auth-filter-service) |

### B → 数据权限方案

| 子方案 | 场景 | 模板锚点 |
|---|---|---|
| B1. `AuthFilterService.fetchModelFilterForRead/Write/Delete` | 最常用，直接返回 RSQL 字符串 | [examples.md#data-permission-filter](./references/examples.md#data-permission-filter) |
| B2. `AbstractPlaceHolderParser` 自定义 RSQL 占位符 | 动态值（当前用户/部门/租户） | [examples.md#placeholder-parser](./references/examples.md#placeholder-parser) |
| B3. 编程式 `AuthApiHolder` 获取数据权限 | 业务代码内获取过滤条件 | [examples.md#programmatic-data-permission](./references/examples.md#programmatic-data-permission) |
| B4. 替换 `DataPermissionApi` | 完全自定义数据权限核心实现 | [examples.md#data-permission-api](./references/examples.md#data-permission-api) |

### C → 登录 / 用户扩展

| 子方案 | 场景 | 模板锚点 |
|---|---|---|
| C1. `UserCookieLogin` | SSO 对接、第三方 token 换登录 | [examples.md#user-cookie-login](./references/examples.md#user-cookie-login) |
| C2. `PamirsUserTransientExtPoint` | 登录前后钩子、强制改密 | [examples.md#login-ext-point](./references/examples.md#login-ext-point) |
| C3. `UserPatternCheckApi` | 密码 / 用户名 / 邮箱等规则 | [examples.md#password-check](./references/examples.md#password-check) |
| C4. `AuthRoleManagerExtPoint` | 角色删除/启用/禁用钩子 | [examples.md#role-manager-ext-point](./references/examples.md#role-manager-ext-point) |
| C5. `CustomCurrentRolesFetcher` | 动态角色（绕过平台缓存） | [examples.md#custom-roles-fetcher](./references/examples.md#custom-roles-fetcher) |

### D → 高级扩展

| 子方案 | 场景 | 模板锚点 |
|---|---|---|
| D1. `PermissionNodeLoadExtendApi` | 权限树节点增删改 | [examples.md#permission-node-load](./references/examples.md#permission-node-load) |
| D2. `AuthResourceAuthorizationExtendApi` | 授权变更同步外部系统 | [examples.md#authorization-extend](./references/examples.md#authorization-extend) |
| D3. `AuthPermissionCacheExtendApi` | 自定义缓存刷新 | [examples.md#cache-extend](./references/examples.md#cache-extend) |
| D4. 运行时 API 替换 | 替换 `DataPermissionApi` 等核心 SPI | [examples.md#data-permission-api](./references/examples.md#data-permission-api) |

## 扩展点速查

最常用的 7 类（完整 14+ 类见 [references/reference.md#扩展点完整一览](./references/reference.md#扩展点完整一览)）：

| 扩展点 | 接口 | 典型场景 | 注册 |
|---|---|---|---|
| 权限过滤 | `AuthFilterService` | 白名单、数据权限、自定义鉴权 | `@SPI.Service` + `@Component` + `@Order` |
| SSO 登录 | `UserCookieLogin` | 第三方 SSO | `@Component` + `@Order(0)` |
| 登录流程 | `PamirsUserTransientExtPoint` | 登录后钩子 / 强制改密 | `@Ext` + `@ExtPoint.Implement` |
| 数据权限 API | `DataPermissionApi` | 替换数据权限核心实现 | `@SPI.Service` + `@Component` |
| 权限树 | `PermissionNodeLoadExtendApi` | 添加/删除权限节点 | `@SPI.Service` + `@Component` + `@Order` |
| RSQL 占位符 | `AbstractPlaceHolderParser` | 自定义数据权限变量 | `@Component` |
| Session | `SessionInitApi` | 注入自定义上下文 | `@Component` |

## 已知陷阱

每条都有「具体表现」（怎么识别已踩坑）和「应对」（怎么避免/修复）。编码前先扫一遍。

| 陷阱 | 具体表现 | 应对 |
|---|---|---|
| `AuthFilterService` 三态返回值 | 未匹配分支返回 `false`，全员被拦 | 未匹配返回 `null`；`true` 才放行短路，`false` 是拒绝短路 |
| 数据权限漏配 Write/Delete | 只配 Read，出现"能查不能改"或"能查能删别人" | 三方法同时实现；不限制时显式 `AuthResult.success(null)` |
| `AuthResult.success(null)` vs `null` 混用 | 链路行为不可预测 | `success(null)`=放行不过滤；`null`=不管交下一个，语义不可换 |
| `UserCookieLogin` Order 缺失 | 自定义实现永远不触发，被默认实现先消费 | `@Order(0)` 显式声明；仅对自己的 loginType 接管，其他返回 null |
| `CustomCurrentRolesFetcher` 性能 | 每请求都触发，QPS 一高把 DB/RPC 打爆 | 仅动态角色场景用；实现内部加 LRU/TTL 缓存 |
| SSO Token 信任客户端 | 客户端传 token / userId 直接登录，等于无鉴权 | 必须调 SSO `userinfo` 服务端验证后再写 Session；Session 写用 `setUserInfoToCookiesAndSetUserIdToCache` 或 `PamirsUserSession.setUser` |
| 数据权限挂在 PROXY 模型 | RSQL 注入时 `` `null` AS `xxx` `` 报错 | 数据权限只挂真实持久化模型 |
| YAML namespace 误填 Java 包 | 白名单不生效 | `namespace` 是模型编码（`MODEL_MODEL`），`fun` 是函数编码（`@Function`/`@Action` 注册名），都不是 Java 包/方法 |
| 运行时 API 替换波及全局 | 替换 `DataPermissionApi` 后别处模型挂掉 | 替换前列全部使用方并自检覆盖；优先用扩展点而非替换 |
| RSQL 占位符注入 | 拼用户输入到 RSQL 等于 SQL 注入 | 占位符值用枚举/白名单；外部输入先校验 |
| JDK 9+ 语法溜进来 | 模块编译失败 | 全员 JDK 8 语法：无 `var` / 文本块 / `switch` 表达式 |
| 设计器配置看不见 | grep 不到弹窗/字段联动 = 误判为未配 | Pamirs 设计器配置存 DB，去设计器 UI 看 |
| 白名单过宽 | 给整个 namespace 免登录，攻击面扩大 | 白名单最小化；优先 `fun-filter-only-login` 而非 `fun-filter`；只白单点函数 |

## 关键约定

1. **SPI 类把 `@Component` + `@SPI.Service` 打在同一个类上**（不是分散到两个类）；ExtPoint 实现用 `@Ext` + `@ExtPoint.Implement`。工具方法可抽出来只挂 `@Component`，但 SPI 入口类两个注解必须齐备，否则 Pamirs SPI 扫描直接漏掉。
2. 多个 `AuthFilterService` 通过 `@Order` 控制优先级，返回 `null` 表示交给下一个，**只有 `true` 才会短路放行**。
3. 数据权限返回 RSQL 字符串，平台自动注入查询条件；不需要过滤时返回 `AuthResult.success(null)`，不是空字符串或 `null` 字面量。
4. `UserCookieLogin` 必须 `@Order(0)` 确保优先于默认实现执行。
5. 扩展实现内抛 `PamirsException` + `ExpEnumerate` 错误码中断流程；禁止 `appendMsg(...)` 拼字符串绕过枚举。
6. 关键类全限定名见 [references/reference.md#关键类速查表](./references/reference.md#关键类速查表)。

## 进一步阅读

加载策略遵循 progressive disclosure，按需打开：

- **[references/reference.md](./references/reference.md)** — 查接口签名、`AuthFilterService` 完整方法表、`AuthResult` 语义、`PamirsUserTransientExtPoint` 钩子列表、YAML 高级配置、SSO 端点细节、运行时 SPI 全限定名、关键类速查表时加载。
- **[references/examples.md](./references/examples.md)** — 决策树已定方案，需要照搬可编译运行的 Java/YAML 模板时加载。按决策树锚点（如 `#auth-filter-service`、`#data-permission-filter`）跳转，避免读全文。

## 文档

- 开发手册：https://guide.oinone.top
- 查询官方文档：https://doc.oinone.top/?s=权限
