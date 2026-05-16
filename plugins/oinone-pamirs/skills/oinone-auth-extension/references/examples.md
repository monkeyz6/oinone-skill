# 权限扩展代码示例

> 导航：[SKILL.md](../SKILL.md) | [reference.md](./reference.md) | **examples.md**
>
> **何时加载本文件：** 已确定扩展类型（YAML 白名单 / AuthFilterService / 数据权限 / SSO / 角色管理 / 授权扩展 / 运行时 API 替换等），需要照搬可直接编译运行的 Java/YAML 模板时打开。按 SKILL.md 决策树给出的锚点跳转到对应小节，避免读全文。

## 目录

- [1. 白名单配置](#1-白名单配置)
- [2. 数据权限](#2-数据权限)
- [3. 权限树扩展](#3-权限树扩展)
- [4. 登录与 SSO](#4-登录与-sso)
- [5. 角色管理](#5-角色管理)
- [6. 授权与缓存扩展](#6-授权与缓存扩展)
- [7. Session 与上下文](#7-session-与上下文)
- [8. 运行时 API 替换](#8-运行时-api-替换)
- [9. 常见扩展场景](#9-常见扩展场景)
- [10. 调试与多租户](#10-调试与多租户)

---

## 1. 白名单配置

### 1.1 YAML 白名单配置 {#yaml-whitelist}

**免登录白名单**（完全跳过权限验证，无需登录）：

```yaml
pamirs:
  auth:
    fun-filter:
      - namespace: user.PamirsUserTransient
        fun: login
      - namespace: demo.DemoModel
        fun: publicQuery
```

**仅需登录白名单**（需要登录但不验证具体权限）：

```yaml
pamirs:
  auth:
    fun-filter-only-login:
      - namespace: demo.DemoModel
        fun: queryList
```

`namespace` 是模型编码（`MODEL_MODEL`），`fun` 是函数编码。

### 1.2 AuthFilterService 完整实现 {#auth-filter-service}

模块级/函数级/路径级免鉴权：

```java
package pro.shushi.pamirs.demo.core.auth;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.auth.api.spi.AuthFilterService;
import pro.shushi.pamirs.meta.common.spi.SPI;

@Order(88)
@Component
@SPI.Service
public class DemoAuthFilterService implements AuthFilterService {

    // 模块级免鉴权
    @Override
    public Boolean isAccessModule(String module) {
        if ("demo_module".equals(module)) {
            return Boolean.TRUE;
        }
        return null; // 交给其他过滤器
    }

    // 函数级免鉴权
    @Override
    public Boolean isAccessFunction(String namespace, String fun) {
        if ("demo.DemoModel".equals(namespace) && "publicQuery".equals(fun)) {
            return Boolean.TRUE;
        }
        return null;
    }

    // 路径级免鉴权
    @Override
    public Boolean isAccessAction(String actionPath) {
        if (actionPath != null && actionPath.startsWith("/public/")) {
            return Boolean.TRUE;
        }
        return null;
    }
}
```

**关键接口：** `pro.shushi.pamirs.auth.api.spi.AuthFilterService`

### 1.3 强制要求登录 {#check-login}

```java
@Override
public Boolean isAccessFunction(String namespace, String fun) {
    if ("demo.DemoModel".equals(namespace) && "myAction".equals(fun)) {
        AuthVerificationHelper.checkLogin(); // 未登录则抛异常
        return Boolean.TRUE; // 已登录则放行，不检查具体权限
    }
    return null;
}
```

**工具类：** `pro.shushi.pamirs.auth.api.utils.AuthVerificationHelper`

### 1.4 获取当前请求上下文 {#request-context}

```java
import pro.shushi.pamirs.boot.web.session.AccessResourceInfoSession;
import pro.shushi.pamirs.boot.web.loader.path.AccessResourceInfo;

AccessResourceInfo info = AccessResourceInfoSession.getInfo();
// info.getModule()     — 当前模块
// info.getModel()      — 当前模型
// info.getActionName() — 当前动作名
// info.getOriginPath() — 原始请求路径
```

---

## 2. 数据权限

### 2.1 行级过滤完整实现 {#data-permission-filter}

```java
@Order(88)
@Component
@SPI.Service
public class DemoDataPermissionFilter implements AuthFilterService {

    @Override
    public AuthResult<String> fetchModelFilterForRead(String model) {
        if ("demo.DemoOrder".equals(model)) {
            Long userId = PamirsSession.getUserId();
            if (userId == null) return null;
            return AuthResult.success("createUid == " + userId);
        }
        return null; // 不处理其他模型
    }

    @Override
    public AuthResult<String> fetchModelFilterForWrite(String model) {
        // 写权限过滤，逻辑同上
        return null;
    }

    @Override
    public AuthResult<String> fetchModelFilterForDelete(String model) {
        // 删除权限过滤
        return null;
    }
}
```

### 2.2 自定义 RSQL 占位符 {#placeholder-parser}

```java
package pro.shushi.pamirs.demo.core.placeholder;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.api.session.PamirsSession;
import pro.shushi.pamirs.core.common.placeholder.AbstractPlaceHolderParser;

@Component
public class CurrentDeptPlaceholder extends AbstractPlaceHolderParser {

    @Override
    public String namespace() {
        return "currentDeptId"; // 占位符名称：${currentDeptId}
    }

    @Override
    protected String value() {
        // 从 Session 扩展属性中获取部门 ID
        String deptId = PamirsSession.getTransmittableExtend().get("deptId");
        return deptId != null ? deptId : "0";
    }

    @Override
    public Integer priority() {
        return 10;
    }

    @Override
    public Boolean active() {
        return Boolean.TRUE;
    }
}
```

使用方式：在数据权限配置中写 `deptId == ${currentDeptId}`。

**平台内置占位符：**
- `${currentUser}` — 当前用户 ID
- `${currentRoles}` — 当前用户角色 ID 集合

### 2.3 编程式获取数据权限 {#programmatic-data-permission}

```java
import pro.shushi.pamirs.meta.api.core.auth.AuthApi;

// 获取某模型的读权限 RSQL 过滤条件
Result<String> result = AuthApi.get().canReadableData("demo.DemoModel");
String rsqlFilter = result.getData();
```

---

## 3. 权限树扩展

### 3.1 PermissionNodeLoadExtendApi 完整实现 {#permission-node-load}

```java
package pro.shushi.pamirs.demo.core.auth;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.auth.api.entity.node.PermissionNode;
import pro.shushi.pamirs.auth.api.entity.node.ModulePermissionNode;
import pro.shushi.pamirs.auth.api.extend.load.PermissionNodeLoadExtendApi;
import pro.shushi.pamirs.auth.api.loader.entity.PermissionLoadContext;
import pro.shushi.pamirs.meta.common.spi.SPI;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Component
@Order(88)
@SPI.Service
public class DemoPermissionNodeExtend implements PermissionNodeLoadExtendApi {

    // 全量加载时调用（权限配置页面初始化）
    @Override
    public List<PermissionNode> buildAllPermissions(
            PermissionLoadContext loadContext,
            List<PermissionNode> nodes,
            Set<Long> roleIds) {
        return buildRootPermissions(loadContext, nodes);
    }

    // 构建根权限节点
    @Override
    public List<PermissionNode> buildRootPermissions(
            PermissionLoadContext loadContext,
            List<PermissionNode> nodes) {
        // 示例：删除某个默认首页节点
        for (PermissionNode node : nodes) {
            if (node instanceof ModulePermissionNode) {
                Iterator<PermissionNode> it = node.getNodes().iterator();
                while (it.hasNext()) {
                    PermissionNode child = it.next();
                    if ("不需要的节点".equals(child.getDisplayValue())) {
                        it.remove();
                    }
                }
            }
        }
        return nodes;
    }

    // 懒加载下级节点时调用
    @Override
    public List<PermissionNode> buildNextPermissions(
            PermissionNode selected,
            List<PermissionNode> nodes) {
        return nodes;
    }
}
```

**接口：** `pro.shushi.pamirs.auth.api.extend.load.PermissionNodeLoadExtendApi`

### 3.2 PermissionNodeConvertExtendApi 完整实现 {#permission-node-convert}

```java
@Component
@SPI.Service
public class DemoNodeConvertExtend implements PermissionNodeConvertExtendApi {

    @Override
    public void convertModuleNode(ModulePermissionNode node, UeModule module) {
        // 自定义模块节点的显示名称或属性
    }

    @Override
    public void convertMenuNode(MenuPermissionNode node, Menu menu) {
        // 自定义菜单节点
    }

    @Override
    public void convertActionNode(ActionPermissionNode node,
            AuthCompileContext context, UIAction actionNode, Action action) {
        // 自定义动作节点
    }
}
```

**接口：** `pro.shushi.pamirs.auth.api.extend.load.PermissionNodeConvertExtendApi`

### 3.3 AuthFetchPermissionService 完整实现 {#fetch-permission-service}

```java
@Component
@SPI.Service
public class DemoFetchPermissionService implements AuthFetchPermissionService {

    @Override
    public Set<String> fetchActionPermissions(Set<String> result, Set<Long> roleIds) {
        // 为特定角色动态添加额外的动作权限
        if (roleIds.contains(SPECIAL_ROLE_ID)) {
            result.add("demo.DemoModel#specialAction");
        }
        return result;
    }

    @Override
    public Set<String> fetchModulePermissions(Set<String> result, Set<Long> roleIds) {
        return null; // 不扩展模块权限
    }
}
```

**接口：** `pro.shushi.pamirs.auth.api.extend.permission.AuthFetchPermissionService`

---

## 4. 登录与 SSO

### 4.1 UserCookieLogin — SSO 对接完整实现 {#user-cookie-login}

```java
package pro.shushi.pamirs.demo.core.auth;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.user.api.login.UserCookieLogin;
import pro.shushi.pamirs.user.api.model.PamirsUser;
import pro.shushi.pamirs.user.api.enmu.UserLoginTypeEnum;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(0)
public class DemoSSOCookieLogin extends UserCookieLogin<PamirsUser> {

    @Override
    public String type() {
        return UserLoginTypeEnum.COOKIE.value();
    }

    @Override
    public PamirsUser fetchUserIdByReq(HttpServletRequest request,
                                        HttpServletResponse response) {
        // 1. 从 Header 或参数获取 SSO token
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }
        if (StringUtils.isBlank(token)) return null;

        // 2. 调用 SSO 服务端验证 token，获取用户信息
        SSOUserInfo ssoUser = ssoService.verify(token);
        if (ssoUser == null) return null;

        // 3. 查找或创建本地用户
        PamirsUser user = findOrCreateUser(ssoUser);

        // 4. 写入 Session 和 Cookie
        return setUserInfoToCookiesAndSetUserIdToCache(
            request, response, user);
    }

    @Override
    protected PamirsUser resolveAndVerification(HttpServletRequest request,
                                                  HttpServletResponse response) {
        // 自定义 token 续期或二次验证逻辑
        return null;
    }
}
```

**基类：** `pro.shushi.pamirs.user.api.login.UserCookieLogin`
**参考实现：** `pro.shushi.pamirs.user.api.login.UserCookieLoginFree`

### 4.2 PamirsUserTransientExtPoint — 登录流程扩展 {#login-ext-point}

```java
package pro.shushi.pamirs.demo.core.auth;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Ext;
import pro.shushi.pamirs.meta.annotation.ExtPoint;
import pro.shushi.pamirs.user.api.model.PamirsUserTransient;
import pro.shushi.pamirs.user.api.extpoint.PamirsUserTransientExtPoint;
import pro.shushi.pamirs.meta.api.session.PamirsSession;

@Order(0)
@Component
@Ext(PamirsUserTransient.class)
public class DemoLoginExtPoint implements PamirsUserTransientExtPoint {

    @Override
    @ExtPoint.Implement
    public PamirsUserTransient loginAfter(PamirsUserTransient user) {
        // 首次登录强制修改密码
        Long userId = PamirsSession.getUserId();
        DemoUser demoUser = new DemoUser().queryById(userId);
        if (Boolean.TRUE.equals(demoUser.getFirstLogin())) {
            user.setBroken(Boolean.TRUE);
            user.setErrorCode("FIRST_LOGIN_CHANGE_PASSWORD");
        }
        return user;
    }

    @Override
    @ExtPoint.Implement
    public PamirsUserTransient loginCustomAfter(PamirsUserTransient user) {
        return user;
    }
}
```

### 4.3 UserPatternCheckApi — 密码规则 {#password-check}

```java
@SPI.Service
@Order(50)
@Component
public class DemoPasswordRule implements UserPatternCheckApi {

    @Override
    public Boolean checkPassword(String password) {
        if (password.length() < 8 || password.length() > 20) {
            throw PamirsException.construct(
                DemoExpEnum.PASSWORD_LENGTH_ERROR).errThrow();
        }
        // 必须包含大小写字母和数字
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw PamirsException.construct(
                DemoExpEnum.PASSWORD_COMPLEXITY_ERROR).errThrow();
        }
        return Boolean.TRUE;
    }
}
```

**接口：** `pro.shushi.pamirs.user.api.UserPatternCheckApi`（`@SPI`）

---

## 5. 角色管理

### 5.1 AuthRoleManagerExtPoint 完整实现 {#role-manager-ext-point}

```java
package pro.shushi.pamirs.demo.core.auth;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.auth.api.extpoint.AuthRoleManagerExtPoint;
import pro.shushi.pamirs.auth.api.model.AuthRole;
import pro.shushi.pamirs.auth.api.model.AuthUserRoleRel;
import pro.shushi.pamirs.auth.api.service.manager.AuthRoleManager;
import pro.shushi.pamirs.meta.annotation.Ext;
import pro.shushi.pamirs.meta.annotation.ExtPoint;

import java.util.List;

@Order(0)
@Component
@Ext(AuthRoleManager.class)
public class DemoRoleManagerExtPoint implements AuthRoleManagerExtPoint {

    @Override
    @ExtPoint.Implement
    public Boolean deleteBefore(List<AuthRole> roles, List<AuthUserRoleRel> userRoleRelList) {
        // 删除角色前校验：如角色下有用户则禁止删除
        if (!userRoleRelList.isEmpty()) {
            throw new RuntimeException("角色下存在用户，无法删除");
        }
        return Boolean.TRUE;
    }

    @Override
    @ExtPoint.Implement
    public Boolean deleteAfter(List<AuthRole> roles, List<AuthUserRoleRel> userRoleRelList) {
        // 删除角色后清理：如清除关联的业务数据
        return Boolean.TRUE;
    }

    @Override
    @ExtPoint.Implement
    public Boolean activeAfter(AuthRole role, List<AuthUserRoleRel> userRoleRelList) {
        // 启用角色后的逻辑
        return Boolean.TRUE;
    }

    @Override
    @ExtPoint.Implement
    public Boolean disableAfter(AuthRole role, List<AuthUserRoleRel> userRoleRelList) {
        // 禁用角色后的逻辑
        return Boolean.TRUE;
    }
}
```

### 5.2 AuthRoleManagerExtendApi — 角色管理 SPI {#role-manager-spi}

与 ExtPoint 类似但通过 SPI 机制注册，适合不依赖 ExtPoint 框架的场景：

```java
@Component
@SPI.Service
public class DemoRoleManagerExtend implements AuthRoleManagerExtendApi {

    @Override
    public void delete(List<AuthRole> roles, List<AuthUserRoleRel> userRoleRelList) {
        // 角色删除时的扩展逻辑
    }

    @Override
    public void active(AuthRole role, List<AuthUserRoleRel> userRoleRelList) {
        // 角色启用时的扩展逻辑
    }

    @Override
    public void disable(AuthRole role, List<AuthUserRoleRel> userRoleRelList) {
        // 角色禁用时的扩展逻辑
    }
}
```

**接口：** `pro.shushi.pamirs.auth.api.spi.AuthRoleManagerExtendApi`

### 5.3 CustomCurrentRolesFetcher 完整实现 {#custom-roles-fetcher}

```java
@Component
@SPI.Service
public class DemoCurrentRolesFetcher implements CustomCurrentRolesFetcher {

    @Override
    public Set<Long> fetch() {
        Long userId = PamirsSession.getUserId();
        if (userId == null) return Collections.emptySet();
        // 自定义角色获取逻辑，如根据部门动态分配角色
        return customRoleService.getRoleIdsByUser(userId);
    }
}
```

---

## 6. 授权与缓存扩展

### 6.1 授权扩展完整实现 {#authorization-extend}

```java
@Component
@SPI.Service
public class DemoResourceAuthExtend implements AuthResourceAuthorizationExtendApi {

    @Override
    public String scene() {
        // 适用场景：rbac（权限配置）或 group（系统权限）
        return AuthAuthorizationSceneApi.RBAC_SCENE;
    }

    @Override
    public void updates(Set<Long> roleIds, List<AuthResourceAuthorization> permissions) {
        // 资源权限授权时的扩展逻辑
        // 如：同步权限到外部系统、记录审计日志
    }
}
```

### 6.2 AuthPermissionCacheExtendApi 完整实现 {#cache-extend}

```java
@Component
@SPI.Service
public class DemoCacheExtend implements AuthPermissionCacheExtendApi {

    @Override
    public void authorizeActionPermissionRefreshCache(
            Set<Long> roleIds,
            List<AuthResourceAuthorization> actionAuthorizations,
            boolean override) {
        // 授权动作权限后刷新自定义缓存
    }

    @Override
    public void revokeActionPermissionRefreshCache(
            Set<Long> roleIds,
            List<AuthResourceAuthorization> actionAuthorizations,
            boolean isDelete) {
        // 撤销动作权限后清理自定义缓存
    }
}
```

**接口：** `pro.shushi.pamirs.auth.api.extend.cache.AuthPermissionCacheExtendApi`

---

## 7. Session 与上下文

### 7.1 SessionInitApi 完整实现 {#session-init}

```java
@Component
public class DemoSessionInitApi implements SessionInitApi {

    @Override
    public void init(HttpServletRequest request,
                     String moduleName,
                     PamirsRequestParam requestParam) {
        // 从请求中提取部门信息并放入 Session
        String deptCode = resolveDeptCode();
        if (StringUtils.isNotBlank(deptCode)) {
            PamirsSession.getTransmittableExtend()
                .put("deptCode", deptCode);
        }
    }
}
```

**接口：** `pro.shushi.pamirs.framework.common.spi.SessionInitApi`

注入的值可在数据权限 RSQL 占位符中使用（配合 `AbstractPlaceHolderParser`）。

### 7.2 AccessPermissionPrepareApi 实现 {#permission-prepare}

```java
@Component
public class DemoPermissionPrepare implements AccessPermissionPrepareApi {

    @Override
    public void prepareAccessPermission(Function function, Object... args) {
        // 在权限校验前预处理，如设置特殊的权限上下文
    }
}
```

**接口：** `pro.shushi.pamirs.auth.api.runtime.spi.AccessPermissionPrepareApi`

---

## 8. 运行时 API 替换

### 8.1 DataPermissionApi 替换完整实现 {#data-permission-api}

```java
@Component
@SPI.Service
@Order(100) // 高于默认实现的优先级
public class CustomDataPermission implements DataPermissionApi {

    @Override
    public AuthResult<Boolean> isAccessModel(String model, List<FunctionTypeEnum> functionType) {
        // 自定义模型访问控制
        return AuthResult.success(Boolean.TRUE);
    }

    @Override
    public AuthResult<Map<String, Long>> fetchFieldPermissions(String model) {
        // 自定义字段权限
        return AuthResult.success();
    }

    @Override
    public AuthResult<String> fetchModelFilterForRead(String model) {
        // 自定义读过滤
        return AuthResult.success();
    }

    @Override
    public AuthResult<String> fetchModelFilterForWrite(String model) {
        return AuthResult.success();
    }

    @Override
    public AuthResult<String> fetchModelFilterForDelete(String model) {
        return AuthResult.success();
    }
}
```

**接口：** `pro.shushi.pamirs.auth.api.runtime.spi.DataPermissionApi`

---

## 9. 常见扩展场景

### 场景 1：某些 API 免登录访问 {#scenario-public-api}

**推荐方案：** YAML `fun-filter` 配置（最简单）

```yaml
pamirs:
  auth:
    fun-filter:
      - namespace: demo.PublicApi
        fun: queryPublicData
```

如果需要动态判断，使用 `AuthFilterService.isAccessFunction()` 返回 `true`。

### 场景 2：已登录用户跳过特定模块的权限校验 {#scenario-skip-module}

**推荐方案：** `AuthFilterService.isAccessModule()`

```java
@Override
public Boolean isAccessModule(String module) {
    if ("demo_public_module".equals(module)) {
        AuthVerificationHelper.checkLogin();
        return Boolean.TRUE;
    }
    return null;
}
```

### 场景 3：按创建人过滤数据 {#scenario-filter-by-creator}

**推荐方案：** `AuthFilterService.fetchModelFilterForRead()`

```java
@Override
public AuthResult<String> fetchModelFilterForRead(String model) {
    if ("demo.DemoOrder".equals(model)) {
        Long userId = PamirsSession.getUserId();
        return AuthResult.success("createUid == " + userId);
    }
    return null;
}
```

### 场景 4：按部门过滤数据 {#scenario-filter-by-dept}

**推荐方案：** `AbstractPlaceHolderParser` + `SessionInitApi` + 数据权限配置

1. 实现 `SessionInitApi` 在请求初始化时注入部门信息
2. 实现 `AbstractPlaceHolderParser` 提供 `${currentDeptId}` 占位符
3. 在数据权限配置中使用 `deptId == ${currentDeptId}`

### 场景 5：对接第三方 SSO {#scenario-sso}

**推荐方案：** 继承 `UserCookieLogin`（见 [4.1](#user-cookie-login)）

### 场景 6：首次登录强制修改密码 {#scenario-force-password}

**推荐方案：** 实现 `PamirsUserTransientExtPoint.loginAfter()`（见 [4.2](#login-ext-point)）

### 场景 7：自定义密码复杂度规则 {#scenario-password-rule}

**推荐方案：** 实现 `UserPatternCheckApi.checkPassword()`（见 [4.3](#password-check)）

### 场景 8：角色删除前校验 {#scenario-role-delete}

**推荐方案：** 实现 `AuthRoleManagerExtPoint.deleteBefore()`（见 [5.1](#role-manager-ext-point)）

### 场景 9：动态角色分配 {#scenario-dynamic-roles}

**推荐方案：** 实现 `CustomCurrentRolesFetcher`（见 [5.3](#custom-roles-fetcher)）

### 场景 10：权限变更时同步到外部系统 {#scenario-sync-external}

**推荐方案：** 实现 `AuthPermissionCacheExtendApi`（见 [6.2](#cache-extend)）或 `AuthResourceAuthorizationExtendApi`（见 [6.1](#authorization-extend)）

---

## 10. 调试与多租户

### 调试权限日志配置 {#debug-logging}

```yaml
logging:
  level:
    pro.shushi.pamirs.auth: DEBUG
    pro.shushi.pamirs.boot.web: DEBUG
```

### 多租户权限处理 {#multi-tenant}

多租户场景下，权限数据按租户隔离。扩展时注意：
- `AuthFilterService` 中通过 `PamirsSession.getTenantId()` 获取当前租户
- 数据权限 RSQL 中可使用租户相关占位符
- 角色获取需考虑租户维度的角色分配
