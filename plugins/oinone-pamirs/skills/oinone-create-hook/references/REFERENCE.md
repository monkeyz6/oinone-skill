# Oinone 函数拦截器 完整注解参数与技术参考

## Import 清单

### HookBefore 完整 import

```java
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookBefore;
import pro.shushi.pamirs.meta.api.dto.fun.Function;

// 可选：依赖注入
import org.springframework.beans.factory.annotation.Autowired;
```

### HookAfter 完整 import

```java
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookAfter;
import pro.shushi.pamirs.meta.api.dto.fun.Function;

// 可选：依赖注入
import org.springframework.beans.factory.annotation.Autowired;
```

**注意**：`@Slf4j` 来自 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j`，是 Pamirs 框架提供的注解，不是 Lombok 的 `lombok.extern.slf4j.Slf4j`。

## @Hook 注解详解

### 注解定义

来源：`pro.shushi.pamirs.meta.annotation.Hook`

作用于方法级别（`@Target({ElementType.METHOD})`），运行时保留。

### 全部参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `priority` | int | 是 | — | 执行优先级，数字越小越先执行。同优先级时执行顺序不确定 |
| `model` | String[] | 否 | 全部模型 | 限定目标模型。使用模型的 `MODEL_MODEL` 常量，支持数组配置多个模型 |
| `fun` | String | 否 | 全部函数 | 限定目标函数编码。对应 `@Function` 注册的函数名 |
| `module` | String[] | 否 | 全部模块 | 限定请求入口模块。指前端 URL 中的模块名，不是拦截器代码所在的模块 |
| `displayName` | String | 否 | "" | 显示名称，用于管理界面和日志展示 |
| `active` | boolean | 否 | true | 是否启用。设为 false 可临时禁用拦截器 |

### priority 使用惯例

| 优先级范围 | 适用场景 | 示例 |
|-----------|---------|------|
| 0-10 | 系统级基础设施（用户会话、权限校验） | UserHook(0)、RoleHook(5)、FunctionPermissionHook(10) |
| 20-40 | 框架级处理（数据权限、RSQL 解析、占位符） | DataPermissionHook(20)、PlaceHolderHook(30) |
| 50-99 | 较高优先级业务逻辑 | — |
| 100-999 | 普通业务拦截器（推荐默认值：100） | — |
| 999 | 数据转换（时区转换等） | TimezoneHookBefore(999)、TimezoneHookAfter(999) |
| `Integer.MAX_VALUE` | 最后执行（日志、审计、缓存清理） | DataAuditUserLogHook、SingletonModelUpdateHookBefore |

## HookBefore 方法签名与返回值

### 方法签名

```java
@Override
@Hook(priority = 100, model = {TargetModel.MODEL_MODEL}, fun = "targetFunctionName")
public Object run(Function function, Object... args)
```

### 参数说明

- `function`：被拦截函数的元数据对象，可通过 `function.getFun()` 获取函数编码，`function.getModule()` 获取模块名，`function.getNamespace()` 获取命名空间（模型编码）
- `args`：被拦截函数的入参数组。`args[0]` 通常是主要的业务数据对象

### 返回值规范

HookBefore 的返回值含义较灵活，框架对返回值没有严格约束：
- 返回 `null`：最常见，不影响后续执行
- 返回 `function`：表示通过校验，继续执行
- 返回 `args`：表示修改了入参并传递
- 抛出 `PamirsException`：阻断执行，返回错误信息给前端

### args 处理模式

```java
// 获取第一个入参（通常是业务数据对象）
if (args != null && args.length > 0 && args[0] != null) {
    TargetModel data = (TargetModel) args[0];
    // 对 data 进行校验或修改
}
```

## HookAfter 方法签名与返回值

### 方法签名

```java
@Override
@Hook(priority = 100, model = {TargetModel.MODEL_MODEL}, fun = "targetFunctionName")
public Object run(Function function, Object ret)
```

### 参数说明

- `function`：被拦截函数的元数据对象（同 HookBefore）
- `ret`：被拦截函数的返回值。**关键点：`ret` 可能是 `Object[]` 类型**，必须做类型判断

### Object[] 解包标准模式

这是 HookAfter 中最重要的处理模式。框架内部可能将返回值包装为 `Object[]`，拦截器必须正确解包：

```java
Object data = null;
if (ret instanceof Object[]) {
    Object[] rets = (Object[]) ret;
    if (rets.length == 1) {
        data = rets[0];
    }
} else {
    data = ret;
}
// 后续使用 data 进行业务处理
```

### 返回值规范

- 通常返回原始 `ret`（不论是否做了修改）
- 如果修改了解包后的 `data`，仍然返回原始 `ret`（因为 `data` 是 `ret` 数组的引用，修改会反映在 `ret` 中）
- 返回 `null` 会导致被拦截函数的返回值被置空，慎用

## 类级别注解

### 标准模式

```java
@Slf4j
@Component
public class FeatureNameHookBefore implements HookBefore {
    // ...
}
```

- `@Component`：Spring 组件注册，必须标注
- `@Slf4j`：Pamirs 日志注解（推荐）

### 可选注解

- `@Base`：标记为基础组件（通常用于框架级拦截器，业务代码不需要）
- `@Order`：Spring 排序注解（与 `@Hook(priority=...)` 功能不同，一般不需要）

## 执行特性

理解以下特性有助于正确使用拦截器：

1. **前端请求触发**：通过 GraphQL 接口发起的前端请求，默认会触发匹配的 Hook
2. **后端调用默认不触发**：后端编程式直接调用函数（如 `service.method()`）时，默认不经过 Hook 链。如需触发，需在调用前设置元位指令：
   ```java
   PamirsSession.directive().enableHook();
   ```
3. **同步执行**：Hook 在被拦截函数的同一线程、同一事务中同步执行
4. **执行顺序**：同类型的 Hook 按 `priority` 从小到大依次执行
5. **HookBefore 抛异常会阻断执行**：如果 HookBefore 抛出异常，被拦截函数不会执行
6. **HookAfter 抛异常不影响已执行的结果**：被拦截函数已执行完毕，但异常会向上传播

## 与其他扩展机制的对比

| 特性 | Hook（拦截器） | ExtPoint（扩展点） | Trigger（触发任务） |
|------|--------------|-------------------|-------------------|
| 执行时机 | 函数执行前/后 | 替换或增强函数实现 | 数据变更后异步 |
| 侵入性 | 非侵入，不修改原函数 | 替换原函数实现 | 非侵入，独立异步 |
| 事务 | 同一事务 | 同一事务 | 独立事务 |
| 同步/异步 | 同步 | 同步 | 异步（MQ） |
| 依赖基础设施 | 无 | 无 | canal + RocketMQ |
| 适用场景 | 校验、日志、数据增强 | 替换默认行为 | 数据同步、通知 |

### 选择指南

- 需要**在函数前做校验或修改入参** → HookBefore
- 需要**在函数后修改返回值或记录日志** → HookAfter
- 需要**替换函数的默认实现** → ExtPoint（不在本 skill 范围）
- 需要**数据变更后异步通知** → Trigger（使用 oinone-create-trigger-task skill）