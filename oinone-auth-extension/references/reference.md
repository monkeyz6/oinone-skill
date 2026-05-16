# 权限扩展参考手册

> 导航：[SKILL.md](../SKILL.md) | **reference.md** | [examples.md](./examples.md)
>
> **何时加载本文件：** 需要查询扩展点接口签名、AuthFilterService 方法列表、AuthResult 语义、PamirsUserTransientExtPoint 钩子、运行时 SPI 全限定名、YAML `pamirs.auth.*` 配置项、SSO 端点细节、关键类速查表时打开。

## 扩展点完整一览

| 扩展点 | 接口/类 | 用途 | 注册方式 |
|--------|---------|------|----------|
| 权限过滤 | `AuthFilterService` | 自定义鉴权规则、白名单、数据权限 | `@SPI.Service` + `@Component` + `@Order` |
| 权限树扩展 | `PermissionNodeLoadExtendApi` | 添加/修改/删除权限管理界面的节点 | `@SPI.Service` + `@Component` + `@Order` |
| 权限节点转换 | `PermissionNodeConvertExtendApi` | 自定义权限节点的转换逻辑 | `@SPI.Service` + `@Component` |
| 权限获取扩展 | `AuthFetchPermissionService` | 扩展权限集合的获取逻辑 | `@SPI.Service` + `@Component` |
| 缓存扩展 | `AuthPermissionCacheExtendApi` | 授权/撤销时的缓存刷新扩展 | `@SPI.Service` + `@Component` |
| 资源授权扩展 | `AuthResourceAuthorizationExtendApi` | 资源权限授权时的扩展逻辑 | `@SPI.Service` + `@Component` |
| 字段授权扩展 | `AuthFieldAuthorizationExtendApi` | 字段权限授权时的扩展逻辑 | `@SPI.Service` + `@Component` |
| 行授权扩展 | `AuthRowAuthorizationExtendApi` | 行权限授权时的扩展逻辑 | `@SPI.Service` + `@Component` |
| 角色管理扩展点 | `AuthRoleManagerExtPoint` | 角色删除/启用/禁用的前后钩子 | `@Ext` + `@ExtPoint.Implement` |
| 角色管理 SPI | `AuthRoleManagerExtendApi` | 角色删除/启用/禁用的扩展 | `@SPI.Service` + `@Component` |
| 数据权限 API | `DataPermissionApi` | 替换数据权限的核心实现 | `@SPI.Service` + `@Component` |
| 访问权限 API | `AccessPermissionApi` | 替换访问权限的核心实现 | `@SPI.Service` + `@Component` |
| 管理权限 API | `ManagementPermissionApi` | 替换管理权限的核心实现 | `@SPI.Service` + `@Component` |
| 权限验证 API | `VerificationPermissionApi` | 替换权限验证的核心实现 | `@SPI.Service` + `@Component` |
| 角色获取 | `CurrentRolesFetcherApi` | 自定义当前用户角色获取逻辑 | `@SPI.Service` + `@Component` |
| 自定义角色获取 | `CustomCurrentRolesFetcher` | 绕过平台缓存的角色获取 | `@SPI.Service` + `@Component` |
| 权限预处理 | `AccessPermissionPrepareApi` | 权限校验前的预处理 | `@Component` |
| 资源信息预处理 | `AccessResourceInfoPrepareApi` | 访问资源信息的预处理 | `@Component` |
| SSO 登录 | `UserCookieLogin` | 自定义登录/SSO 对接 | `@Component` + `@Order` |
| 密码规则 | `UserPatternCheckApi` | 自定义密码校验规则 | `@SPI.Service` + `@Component` |
| 登录流程扩展 | `PamirsUserTransientExtPoint` | 登录前后/密码修改的扩展 | `@Ext` + `@ExtPoint.Implement` |
| Session 初始化 | `SessionInitApi` | 请求会话初始化扩展 | `@Component` |
| RSQL 占位符 | `AbstractPlaceHolderParser` | 自定义数据权限占位符 | `@Component` |

## Hook 执行链

```
请求进入
  ↓
RoleHook (priority=5, HookBefore)
  → 初始化当前用户角色 Session
  → 解析请求路径为 AccessResourceInfo
  → 调用所有 AccessPermissionPrepareApi 预处理
  ↓
FunctionPermissionHook (priority=10, HookBefore)
  → 检查函数/动作级别的访问权限
  → 调用 AuthApi 进行权限校验
  → 无权限时抛出异常
  ↓
DataPermissionHook (priority=20, HookBefore)
  → 准备数据权限过滤条件（RSQL）
  → 注入到查询参数中
  ↓
[业务逻辑执行]
  ↓
FieldPermissionHook (HookAfter)
  → 对返回结果进行字段级权限过滤
```

## AuthFilterService 协作机制

多个 `AuthFilterService` 实现按 `@Order` 排序依次执行：
- 返回 `true`：验证通过，不再执行后续过滤器
- 返回 `false`：验证失败，拒绝访问
- 返回 `null`：交给下一个过滤器处理

**只要有一个过滤器返回 `true`，即可正常访问。**

### AuthFilterService 完整方法列表

| 方法 | 参数 | 用途 |
|------|------|------|
| `isAccessModule(String module)` | 模块编码 | 模块访问控制 |
| `isAccessHomepage(String module)` | 模块编码 | 首页访问控制 |
| `isAccessMenu(String module, String name)` | 模块编码, 菜单名 | 菜单访问控制 |
| `isAccessFunction(String namespace, String fun)` | 命名空间, 函数编码 | 函数访问控制 |
| `isAccessAction(String model, String name)` | 模型编码, 动作名 | 动作访问控制（按模型+名称） |
| `isAccessAction(String actionPath)` | 动作资源路径 | 动作访问控制（按路径） |
| `isAccessModel(String model, List<FunctionTypeEnum> functionTypes)` | 模型编码, 函数类型 | 模型访问控制 |
| `fetchFieldPermissions(String model)` | 模型编码 | 字段权限获取 |
| `fetchModelFilterForRead(String model)` | 模型编码 | 读数据权限过滤（RSQL） |
| `fetchModelFilterForWrite(String model)` | 模型编码 | 写数据权限过滤（RSQL） |
| `fetchModelFilterForDelete(String model)` | 模型编码 | 删除数据权限过滤（RSQL） |

## AuthResult 语义

- `AuthResult.success("rsql")` — 成功获取，应用此 RSQL 过滤
- `AuthResult.success()` / `AuthResult.success(null)` — 成功获取，无需过滤（放行）
- `AuthResult.error()` — 获取失败，交给其他过滤器
- 返回 `null` — 不处理，交给其他过滤器

## PamirsUserTransientExtPoint 完整方法列表

- `loginAfter` — 登录成功后
- `loginCustomAfter` — 自定义登录成功后
- `firstResetPasswordBefore/After` — 首次重置密码前后
- `modifyCurrentUserPasswordBefore/After` — 修改密码前后

**关键字段：**
- `user.setBroken(true)` — 中断登录流程
- `user.setErrorCode(code)` — 设置错误码，前端据此跳转

## UserPatternCheckApi 可校验项

`checkPassword`, `checkLogin`, `checkEmail`, `checkPhone`, `checkName`, `checkNickName`, `checkRealName`, `checkIdCard`, `checkInitialPassword`, `checkContactEmail`

## 三个角色获取 SPI 的区别

- `CurrentRolesFetcher` — 基础角色获取，平台内部使用
- `CurrentRolesFetcherApi` — 标准角色获取 API，使用平台缓存
- `CustomCurrentRolesFetcher` — 自定义角色获取，绕过平台缓存，适合动态角色场景

## 授权扩展场景控制（AuthAuthorizationSceneApi）

- `RBAC_SCENE = "rbac"` — 权限配置场景
- `GROUP_SCENE = "group"` — 系统权限场景
- `isExecution(scene)` — 默认在非自身场景时执行（即 rbac 扩展在 group 场景执行，反之亦然）

**三个授权扩展接口：**
- `AuthResourceAuthorizationExtendApi` — 资源权限（模块/菜单/动作）授权扩展
- `AuthFieldAuthorizationExtendApi` — 字段权限授权扩展
- `AuthRowAuthorizationExtendApi` — 行权限授权扩展

## 缓存方法命名规则

- `authorize{Type}PermissionRefreshCache` — 授权时刷新缓存
- `revoke{Type}PermissionRefreshCache` — 撤销时刷新缓存
- Type: Module / Homepage / Menu / Action / Model / Field / Row
- 每种类型有两个重载：带 `roleIds` 参数和不带的版本

## AccessPermissionPrepareApi

在 `RoleHook` 执行后、`FunctionPermissionHook` 执行前，对权限上下文进行预处理。

**接口：** `pro.shushi.pamirs.auth.api.runtime.spi.AccessPermissionPrepareApi`

## 运行时权限 API 替换

以下 SPI 可替换权限系统的核心实现，属于高级扩展，需谨慎使用。

### 运行时 API 接口列表

| 接口 | 全限定名 | 用途 |
|------|----------|------|
| `DataPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.DataPermissionApi` | 数据权限核心（模型访问/字段权限/行过滤） |
| `AccessPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.AccessPermissionApi` | 访问权限判断（模块/菜单/动作/函数） |
| `ManagementPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.ManagementPermissionApi` | 管理权限判断（可分配的权限范围） |
| `VerificationPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.VerificationPermissionApi` | 底层权限验证逻辑 |
| `FetchPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.FetchPermissionApi` | 通用权限获取框架 |

### 缓存层 API 接口列表

| 接口 | 全限定名 | 用途 |
|------|----------|------|
| `AccessPermissionCacheApi` | `pro.shushi.pamirs.auth.api.runtime.cache.AccessPermissionCacheApi` | 访问权限缓存 |
| `DataPermissionCacheApi` | `pro.shushi.pamirs.auth.api.runtime.cache.DataPermissionCacheApi` | 数据权限缓存 |
| `ManagementPermissionCacheApi` | `pro.shushi.pamirs.auth.api.runtime.cache.ManagementPermissionCacheApi` | 管理权限缓存 |
| `CurrentRolesCacheApi` | `pro.shushi.pamirs.auth.api.runtime.spi.CurrentRolesCacheApi` | 当前角色缓存 |

## YAML 高级配置（AuthConfiguration）

配置前缀：`pamirs.auth`（对应类 `pro.shushi.pamirs.auth.api.configure.AuthConfiguration`）

```yaml
pamirs:
  auth:
    # 权限加载器配置
    loader:
      node-converter: customBeanName    # 自定义节点转换器 Bean 名称
      path-generator: customBeanName    # 自定义路径生成器 Bean 名称
      using-load-cache: true            # 是否使用加载缓存（默认 true）

    # 访问权限配置
    access:
      module-match: true                # 是否启用模块匹配（默认 true）
      resources:                        # 启用的访问权限资源类型
        - MODULE
        - HOMEPAGE
        - MENU
        - VIEW
        - ACTION
        - VIEW_ACTION
        - SERVER_ACTION
        - URL_ACTION
        - CLIENT_ACTION
      strategy:
        loader: customBeanName          # 自定义访问权限加载器

    # 管理权限配置
    management:
      optimize: true                    # 是否优化（默认 true）
      resources:                        # 启用的管理权限资源类型
        - MODULE
        - HOMEPAGE
        - MENU
      strategy:
        loader: customBeanName          # 自定义管理权限加载器
        cache-loader: customBeanName    # 自定义缓存加载器
        field:
          show-all: true                # 字段是否全部显示（默认 true）

    # 数据权限配置
    data:
      resources:                        # 启用的数据权限资源类型
        - MODEL
        - FIELD
        - ROW
      strategy:
        bean-name: customBeanName       # 自定义数据权限策略 Bean

    # 免登录白名单
    fun-filter:
      - namespace: model.namespace
        fun: functionName

    # 仅需登录白名单
    fun-filter-only-login:
      - namespace: model.namespace
        fun: functionName
```

## SSO 集成配置

### 内部 SSO（OAuth2，6.3.x+）

**服务端依赖：**

```xml
<dependency>
    <groupId>pro.shushi.pamirs.core</groupId>
    <artifactId>pamirs-sso-server</artifactId>
</dependency>
```

**客户端依赖：**

```xml
<dependency>
    <groupId>pro.shushi.pamirs.core</groupId>
    <artifactId>pamirs-sso-client-starter</artifactId>
</dependency>
```

```yaml
pamirs:
  sso:
    enabled: true
    client:
      server-url: http://sso-server/pamirs/sso
      client-id: your-client-id
      client-secret: your-client-secret
      exclude-urls:        # 不走 SSO 的路径
        - /public/**
      url-patterns:        # 需要 SSO 的路径
        - /**
```

**核心端点：**
- `GET /pamirs/sso/auth` — 初始认证请求
- `POST /pamirs/sso/oauth2/authorize` — 授权码换 token
- `POST /pamirs/sso/oauth2/getUserInfo` — 通过 token 获取用户信息
- `POST /pamirs/sso/oauth2/logout` — 单点登出

### 外部 SSO（自定义 UserCookieLogin）

见 [examples.md#sso-usercookielogin](./examples.md#sso-usercookielogin)。客户端请求需携带：
```
Authorization: Bearer {access_token}
loginType: OAUTH
```

## 关键类速查表

| 类/接口 | 全限定名 |
|---------|----------|
| `AuthFilterService` | `pro.shushi.pamirs.auth.api.spi.AuthFilterService` |
| `AuthApi` | `pro.shushi.pamirs.meta.api.core.auth.AuthApi` |
| `AuthVerificationHelper` | `pro.shushi.pamirs.auth.api.utils.AuthVerificationHelper` |
| `AccessResourceInfoSession` | `pro.shushi.pamirs.boot.web.session.AccessResourceInfoSession` |
| `AccessResourceInfo` | `pro.shushi.pamirs.boot.web.loader.path.AccessResourceInfo` |
| `AuthConfiguration` | `pro.shushi.pamirs.auth.api.configure.AuthConfiguration` |
| `AuthApiHolder` | `pro.shushi.pamirs.auth.api.holder.AuthApiHolder` |
| `AuthResult` | `pro.shushi.pamirs.auth.api.entity.AuthResult` |
| `PermissionNodeLoadExtendApi` | `pro.shushi.pamirs.auth.api.extend.load.PermissionNodeLoadExtendApi` |
| `PermissionNodeConvertExtendApi` | `pro.shushi.pamirs.auth.api.extend.load.PermissionNodeConvertExtendApi` |
| `AuthFetchPermissionService` | `pro.shushi.pamirs.auth.api.extend.permission.AuthFetchPermissionService` |
| `AuthPermissionCacheExtendApi` | `pro.shushi.pamirs.auth.api.extend.cache.AuthPermissionCacheExtendApi` |
| `AuthResourceAuthorizationExtendApi` | `pro.shushi.pamirs.auth.api.extend.authorization.AuthResourceAuthorizationExtendApi` |
| `AuthFieldAuthorizationExtendApi` | `pro.shushi.pamirs.auth.api.extend.authorization.AuthFieldAuthorizationExtendApi` |
| `AuthRowAuthorizationExtendApi` | `pro.shushi.pamirs.auth.api.extend.authorization.AuthRowAuthorizationExtendApi` |
| `AuthAuthorizationSceneApi` | `pro.shushi.pamirs.auth.api.extend.authorization.AuthAuthorizationSceneApi` |
| `AuthRoleManagerExtPoint` | `pro.shushi.pamirs.auth.api.extpoint.AuthRoleManagerExtPoint` |
| `AuthRoleManagerExtendApi` | `pro.shushi.pamirs.auth.api.spi.AuthRoleManagerExtendApi` |
| `DataPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.DataPermissionApi` |
| `AccessPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.AccessPermissionApi` |
| `ManagementPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.ManagementPermissionApi` |
| `VerificationPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.VerificationPermissionApi` |
| `FetchPermissionApi` | `pro.shushi.pamirs.auth.api.runtime.spi.FetchPermissionApi` |
| `CurrentRolesFetcherApi` | `pro.shushi.pamirs.auth.api.runtime.spi.CurrentRolesFetcherApi` |
| `CustomCurrentRolesFetcher` | `pro.shushi.pamirs.auth.api.runtime.spi.CustomCurrentRolesFetcher` |
| `AccessPermissionPrepareApi` | `pro.shushi.pamirs.auth.api.runtime.spi.AccessPermissionPrepareApi` |
| `AccessResourceInfoPrepareApi` | `pro.shushi.pamirs.auth.api.runtime.spi.AccessResourceInfoPrepareApi` |
| `SessionInitApi` | `pro.shushi.pamirs.framework.common.spi.SessionInitApi` |
| `AbstractPlaceHolderParser` | `pro.shushi.pamirs.core.common.placeholder.AbstractPlaceHolderParser` |
| `UserCookieLogin` | `pro.shushi.pamirs.user.api.login.UserCookieLogin` |
| `UserPatternCheckApi` | `pro.shushi.pamirs.user.api.UserPatternCheckApi` |
| `PamirsUserTransientExtPoint` | `pro.shushi.pamirs.user.api.extpoint.PamirsUserTransientExtPoint` |
| `PamirsSession` | `pro.shushi.pamirs.meta.api.session.PamirsSession` |
