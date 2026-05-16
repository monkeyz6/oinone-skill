# Oinone 异步任务 API 参考

## 目录

1. [@XAsync 注解](#xasync-注解)
2. [@XAsync 底层机制](#xasync-底层机制)
3. [ExecuteTaskAction 类](#executetaskaction-类)
4. [ExecuteTaskActionService 接口](#executetaskactionservice-接口)
5. [ScheduleAction 接口](#scheduleaction-接口)
6. [BaseScheduleNoTransactionTask 基类](#baseschedulenotransactiontask-基类)
7. [TaskType 枚举](#tasktype-枚举)
8. [TimeUnitEnum 枚举](#timeunitenum-枚举)
9. [Context 序列化/反序列化](#context-序列化反序列化)
10. [Import 列表](#import-列表)
11. [FUN_NAMESPACE 命名惯例](#fun_namespace-命名惯例)

---

## @XAsync 注解

```
包路径：pro.shushi.pamirs.trigger.annotation.XAsync
```

### 属性表

| 属性 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `displayName` | String | 是 | `""` | 任务显示名称（中文），用于监控和日志 |
| `limitRetryNumber` | int | 否 | `-1` | 最大重试次数。`-1`=按系统配置，`0`=不重试 |
| `nextRetryTimeValue` | int | 否 | `3` | 重试间隔数值 |
| `nextRetryTimeUnit` | TimeUnitEnum | 否 | `SECOND` | 重试间隔单位 |
| `delayTime` | int | 否 | `0` | 延迟执行时间数值。`0`=立即执行 |
| `delayTimeUnit` | TimeUnitEnum | 否 | `SECOND` | 延迟执行时间单位 |
| `taskType` | String | 否 | `""` | 自定义任务类型（空=框架默认） |

### 当前项目中常见配置组合

**组合 1：最简（无重试无延迟）**
```java
@XAsync(displayName = "异步处理标签任务")
```

**组合 2：标准重试（最常见，约 60% 使用）**
```java
@XAsync(displayName = "异步更新商品标准销售单价", limitRetryNumber = 3, nextRetryTimeValue = 60)
```

**组合 3：短延迟**
```java
@XAsync(displayName = "异步推送银行流水到SAP", limitRetryNumber = 3, delayTime = 30)
```

**组合 4：长延迟（分钟级）**
```java
@XAsync(displayName = "异步调用sap清账接口", limitRetryNumber = 6, delayTime = 20, delayTimeUnit = TimeUnitEnum.MINUTE)
```

**组合 5：分钟级重试间隔**
```java
@XAsync(displayName = "授信结算单的计算生成", limitRetryNumber = 3, nextRetryTimeUnit = TimeUnitEnum.MINUTE, nextRetryTimeValue = 10)
```

**组合 6：不重试**
```java
@XAsync(displayName = "刷新地区数据", limitRetryNumber = 0)
```

---

## @XAsync 底层机制

框架通过 `XAsyncAspect`（AOP 切面）拦截带 `@XAsync` 注解的方法调用：

1. 方法被调用时，切面 `@Around` 拦截
2. 检查会话指令是否强制同步执行（`SystemDirectiveEnum.SYNC`）
3. 若非同步模式，自动构建 `ExecuteTaskAction` 对象（填入注解参数、方法签名、参数序列化）
4. 通过 `executeTaskActionService.submit()` 提交到调度中心
5. 调度中心异步执行，失败按配置重试

**关键限制：** 由于使用 AOP 代理，**同类内自调用（`this.asyncMethod()`）不会触发异步**。必须通过注入的 Service 引用调用。

---

## ExecuteTaskAction 类

```
包路径：pro.shushi.pamirs.trigger.model.ExecuteTaskAction
```

### setter 方法表

| setter 方法 | 类型 | 必填 | 默认值 | 说明 |
|-------------|------|------|--------|------|
| `setDisplayName(String)` | String | 是 | - | 任务显示名称（中文） |
| `setTaskType(String)` | String | 是 | - | 任务类型，通过 `TaskType.XXX.getValue()` 或自定义字符串 |
| `setExecuteNamespace(String)` | String | 是 | - | 执行函数的命名空间（= FUN_NAMESPACE） |
| `setExecuteFun(String)` | String | 是 | - | 执行函数的方法名（= METHOD_NAME，通常为 `"execute"`） |
| `setContext(String)` | String | 否 | null | JSON 格式的上下文数据 |
| `setBizId(Long)` | Long | 否 | - | 业务实体 ID（用于分片/分组） |
| `setBizCode(String)` | String | 否 | - | 业务实体编码（用于分片/分组） |
| `setLimitRetryNumber(Integer)` | Integer | 否 | -1 | 最大重试次数 |
| `setNextRetryTimeValue(Integer)` | Integer | 否 | 0 | 重试间隔数值 |
| `setNextRetryTimeUnit(TimeUnitEnum)` | TimeUnitEnum | 否 | - | 重试间隔单位 |
| `setDelayTimeValue(Integer)` | Integer | 否 | 0 | 延迟执行时间数值 |
| `setDelayTimeUnit(TimeUnitEnum)` | TimeUnitEnum | 否 | - | 延迟执行时间单位 |
| `setFirstExecuteTime(Long)` | Long | 否 | - | 首次执行时间戳（毫秒） |
| `setExecuteFunction(FunctionDefinition)` | FunctionDefinition | 否 | - | 函数定义，主要设超时 `.setTimeout(ms)` |
| `setActive(Boolean)` | Boolean | 否 | - | 是否激活 |

### construct() 方法

`construct()` 是可选的链式调用终结方法，用于生成内部标识。项目中约 50% 的实现调用了它，另 50% 直接 submit。两种方式都可以正常工作。

```java
// 写法 1：调用 construct()
executeTaskActionService.submit(new ExecuteTaskAction()
        .setTaskType(...)
        .setBizId(...)
        .setContext(...)
        .construct());

// 写法 2：不调用 construct()
executeTaskActionService.submit((ExecuteTaskAction) new ExecuteTaskAction()
        .setBizId(...)
        .setTaskType(...)
        .setContext(...));
```

### FunctionDefinition 超时配置

```java
new FunctionDefinition().setTimeout(5000)    // 5 秒超时（默认）
new FunctionDefinition().setTimeout(50000)   // 50 秒超时（中等耗时任务）
new FunctionDefinition().setTimeout(600000)  // 10 分钟超时（长耗时任务）
new FunctionDefinition().setTimeout(-1)      // 无超时限制
```

---

## ExecuteTaskActionService 接口

```
包路径：pro.shushi.pamirs.trigger.service.ExecuteTaskActionService
```

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `submit(ExecuteTaskAction)` | ExecuteTaskAction | Boolean | 提交异步任务 |

**注入方式：**
```java
@Autowired
private ExecuteTaskActionService executeTaskActionService;
```

---

## ScheduleAction 接口

```
包路径：pro.shushi.pamirs.middleware.schedule.api.ScheduleAction
```

```java
public interface ScheduleAction {
    String SEPARATOR_OCTOTHORPE = "#";
    String DEFAULT_METHOD = "execute";

    /** 返回函数命名空间，通常返回 FUN_NAMESPACE 常量 */
    String getInterfaceName();

    /** 执行任务的核心方法 */
    Result<Void> execute(ScheduleItem task);

    /** 返回方法名，默认 "execute" */
    default String getMethodName() {
        return DEFAULT_METHOD;
    }

    /** 返回 actionName = interfaceName + "#" + methodName */
    default String getActionName() {
        return this.getInterfaceName() + SEPARATOR_OCTOTHORPE + this.getMethodName();
    }
}
```

实现类必须：
- 加 `@Component` 注解注册为 Spring Bean
- 加 `@Fun(FUN_NAMESPACE)` 注册到 Pamirs 函数注册表
- `execute()` 方法加 `@Function` 注解
- `execute()` 返回 `Result<Void>`，通过 `result.setSuccess(true/false)` 或 `result.setFail(msg)` 标记结果

### getInterfaceName() 的两种写法

```java
// 写法 1（推荐）：返回 FUN_NAMESPACE
@Override
public String getInterfaceName() {
    return FUN_NAMESPACE;  // 或 XxxService.FUN_NAMESPACE
}

// 写法 2（少数旧代码）：返回 null（不推荐，新代码应避免）
@Override
public String getInterfaceName() {
    return null;
}
```

---

## BaseScheduleNoTransactionTask 基类

```
包路径：pro.shushi.pamirs.middleware.schedule.core.tasks.BaseScheduleNoTransactionTask
```

当需要自定义任务类型时，创建一个继承此基类的 Task 定义类：

```java
@Component
public class MyAsyncTask extends BaseScheduleNoTransactionTask {
    public static final String TASK_TYPE = MyAsyncTask.class.getSimpleName();
    // 或自定义字符串：
    // public static final String TASK_TYPE = "MY_CUSTOM_TASK_TYPE";

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }
}
```

**何时需要自定义 Task 定义类：**
- 需要按任务类型监控/筛选
- 需要在调度系统中区分不同类型的异步任务
- 多个 ScheduleAction 实现共用同一个任务类型

**何时不需要：**
- 直接使用 `TaskType.BASE_SCHEDULE_NO_TRANSACTION_TASK.getValue()` 或 `TaskType.REMOTE_SCHEDULE_TASK.getValue()` 即可

---

## TaskType 枚举

```
包路径：pro.shushi.pamirs.middleware.schedule.eunmeration.TaskType
```

注意：包名是 `eunmeration`（框架原始拼写），不是 `enumeration`。

### 异步任务常用值

| 枚举值 | getValue() | 说明 | 常见用途 |
|--------|------------|------|----------|
| `REMOTE_SCHEDULE_TASK` | `"REMOTE_SCHEDULE_TASK"` | 远程调度任务 | 远程调用、外部系统同步 |
| `BASE_SCHEDULE_NO_TRANSACTION_TASK` | `"BASE_SCHEDULE_NO_TRANSACTION_TASK_KEY"` | 单次异步，无事务 | **最常用于异步任务** |
| `BASE_SCHEDULE_TASK` | `"BASE_SCHEDULE_TASK"` | 基础任务，有事务 | 需要事务控制的操作 |
| `SERIAL_BASE_SCHEDULE_TASK` | - | 按 bizId 分片，有事务 | 大批量有序处理 |
| `SERIAL_BASE_SCHEDULE_NO_TRANSACTION_TASK` | - | 按 bizId 分片，无事务 | 大批量有序处理（无事务） |

**使用方式：** 通过 `.getValue()` 获取字符串值：
```java
action.setTaskType(TaskType.BASE_SCHEDULE_NO_TRANSACTION_TASK.getValue());
```

或使用自定义 Task 定义类的 TASK_TYPE 常量：
```java
action.setTaskType(MyAsyncTask.TASK_TYPE);
```

---

## TimeUnitEnum 枚举

```
包路径：pro.shushi.pamirs.core.common.enmu.TimeUnitEnum
```

注意：包名是 `enmu`（框架原始拼写），不是 `enum`。

| 枚举值 | 显示名 | 说明 |
|--------|--------|------|
| `SECOND` | 秒 | 秒（默认） |
| `MINUTE` | 分钟 | 分钟 |
| `HOUR_OF_DAY` | 时 | 按天计算的时 |
| `DAY_OF_YEAR` | 日 | 按年计算的日 |
| `DAY_OF_MONTH` | 日 | 按月计算的日 |

**异步任务中最常用：** `SECOND`（默认）和 `MINUTE`。

---

## Context 序列化/反序列化

### 模式 1：简单类型（单个 ID）

```java
// 序列化
action.setContext(id.toString());

// 反序列化
Long id = Long.valueOf(scheduleItem.getContext());
```

### 模式 2：复杂对象（JSON）

```java
// 序列化
action.setContext(JsonUtils.toJSONString(requestObject));

// 反序列化
MyRequest request = JsonUtils.parseObject(scheduleItem.getContext(), MyRequest.class);
```

### 模式 3：集合类型（JSON）

```java
// 序列化
action.setContext(JsonUtils.toJSONString(idList));

// 反序列化
List<Long> ids = JsonUtils.parseObject(scheduleItem.getContext(), List.class);
```

**JsonUtils 包路径：** `pro.shushi.pamirs.meta.util.JsonUtils`

---

## Import 列表

### @XAsync 模式 — Interface 侧

```java
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
```

### @XAsync 模式 — Impl 侧

```java
import org.springframework.stereotype.Service;  // 或 Component
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.trigger.annotation.XAsync;
// 若使用非 SECOND 的时间单位：
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;
```

### ExecuteTaskAction 模式 — Task 定义类

```java
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.middleware.schedule.core.tasks.BaseScheduleNoTransactionTask;
```

### ExecuteTaskAction 模式 — ScheduleAction 实现类

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.common.exception.PamirsException;
import pro.shushi.pamirs.meta.util.JsonUtils;
import pro.shushi.pamirs.middleware.schedule.api.ScheduleAction;
import pro.shushi.pamirs.middleware.schedule.common.Result;
import pro.shushi.pamirs.middleware.schedule.domain.ScheduleItem;
import pro.shushi.pamirs.middleware.schedule.eunmeration.TaskType;
import pro.shushi.pamirs.trigger.model.ExecuteTaskAction;
import pro.shushi.pamirs.trigger.service.ExecuteTaskActionService;
// 若需要超时配置：
import pro.shushi.pamirs.meta.domain.fun.FunctionDefinition;
```

---

## FUN_NAMESPACE 命名惯例

**格式：** `"[项目标识].[模块].[可选子包].类名"`

### @XAsync 模式（跟随 Service 接口的 FUN_NAMESPACE）

| 项目 | 模块 | 示例 |
|------|------|------|
| deli-b2b | trade | `"deli.aries.trade.DeliAriesTradeOrderService"` |
| deli-b2b | item | `"deli.aries.item.DeliAriesItemService"` |
| deli-b2b | fund | `"deli.aries.fund.DeliAriesDisBankRecordService"` |
| deli-b2b | major | `"deli.aries.major.DeliAriesDistributorService"` |
| kailas | trade | `"aries.trade.AriesTmsOrderService"` |
| kailas | item | `"aries.item.AriesItemTplPublishService"` |

### ExecuteTaskAction 模式（独立的任务命名空间）

| 项目 | 模块 | 示例 |
|------|------|------|
| deli-b2b | trade | `"aries.trade.schedule.DeliAriesOutTradeOrderAckTask"` |
| deli-b2b | promotion | `"deli.aries.promotion.DeliAriesActivityReportAsyncHandelService"` |
| deli-b2b | item | `"deli.aries.item.schedule.DeliAriesItemAuthScheduleAction"` |
| kailas | settlement | `"aries.settlement.schedule.AriesRebateSettlementManager"` |

**推荐默认格式：**
- @XAsync：`"[项目].[模块].[接口名]"` — 跟随所属 Service 接口
- ExecuteTaskAction：`"[项目].[模块].schedule.[类名]"` — 独立任务命名空间
