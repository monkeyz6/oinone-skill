# Oinone 定时任务 API 参考

## 目录

1. [ScheduleAction 接口](#scheduleaction-接口)
2. [ScheduleTaskAction 字段](#scheduletaskaction-字段)
3. [TimeUnitEnum 枚举](#timeunitenum-枚举)
4. [TriggerTimeAnchorEnum 枚举](#triggertimeanchorenum-枚举)
5. [TaskType 枚举](#tasktype-枚举)
6. [ScheduleTaskActionService 接口](#scheduletaskactionservice-接口)
7. [InstallDataInit / UpgradeDataInit 接口](#installdatainit--upgradedatainit-接口)
8. [Import 列表](#import-列表)
9. [FUN_NAMESPACE 命名惯例](#fun_namespace-命名惯例)
10. [technicalName 命名惯例](#technicalname-命名惯例)
11. [firstExecuteTime 辅助方法](#firstexecutetime-辅助方法)

---

## ScheduleAction 接口

```java
// 包路径：pro.shushi.pamirs.middleware.schedule.api.ScheduleAction
public interface ScheduleAction {
    String SEPARATOR_OCTOTHORPE = "#";
    String DEFAULT_METHOD = "execute";

    /** 返回函数命名空间，通常返回 FUN_NAMESPACE 常量 */
    String getInterfaceName();

    /** 执行定时任务的核心方法 */
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
- `execute()` 方法返回 `Result<Void>`，通过 `result.setSuccess(true/false)` 标记成功/失败

---

## ScheduleTaskAction 字段

| setter 方法 | 类型 | 必填 | 默认值 | 说明 |
|-------------|------|------|--------|------|
| `setDisplayName(String)` | String | 是 | - | 任务显示名称（中文） |
| `setTechnicalName(String)` | String | 是 | - | 技术唯一标识（unique 约束） |
| `setLimitExecuteNumber(Integer)` | Integer | 否 | -1 | 执行次数限制。-1=无限循环，1=单次执行 |
| `setPeriodTimeValue(Integer)` | Integer | 是 | - | 周期数值（如 1、5、15） |
| `setPeriodTimeUnit(TimeUnitEnum)` | TimeUnitEnum | 是 | - | 周期时间单位 |
| `setPeriodTimeAnchor(TriggerTimeAnchorEnum)` | TriggerTimeAnchorEnum | 是 | - | 触发时机锚点 |
| `setLimitRetryNumber(Integer)` | Integer | 否 | 0 | 失败重试次数 |
| `setNextRetryTimeValue(Integer)` | Integer | 否 | 0 | 重试间隔数值 |
| `setNextRetryTimeUnit(TimeUnitEnum)` | TimeUnitEnum | 否 | - | 重试间隔单位 |
| `setExecuteNamespace(String)` | String | 是 | - | 执行函数的命名空间（= FUN_NAMESPACE） |
| `setExecuteFun(String)` | String | 是 | - | 执行函数的方法名（= METHOD_NAME） |
| `setExecuteFunction(FunctionDefinition)` | FunctionDefinition | 否 | - | 函数定义，主要设超时 `.setTimeout(ms)` |
| `setTaskType(String)` | String | 是 | - | 任务类型，通过 `TaskType.XXX.getValue()` 获取 |
| `setContext(String)` | String | 否 | null | JSON 格式的上下文数据 |
| `setActive(Boolean)` | Boolean | 是 | - | 是否激活 |
| `setFirstExecuteTime(Long)` | Long | 是 | - | 首次执行时间戳（毫秒） |
| `setBizId(Long)` | Long | 否 | - | 关联的业务实体 ID |

**FunctionDefinition 常用配置：**
```java
new FunctionDefinition().setTimeout(5000)    // 5 秒超时（默认）
new FunctionDefinition().setTimeout(50000)   // 50 秒超时（中等耗时任务）
new FunctionDefinition().setTimeout(600000)  // 10 分钟超时（长耗时任务）
new FunctionDefinition().setTimeout(-1)      // 无超时限制
```

---

## TimeUnitEnum 枚举

```
包路径：pro.shushi.pamirs.core.common.enmu.TimeUnitEnum
```

注意：包名是 `enmu`（框架原始拼写），不是 `enum`。

| 枚举值 | 显示名 | 说明 | 对应 Calendar 常量 |
|--------|--------|------|-------------------|
| `YEAR` | 年 | 每 N 年 | Calendar.YEAR |
| `MONTH` | 月 | 每 N 月 | Calendar.MONDAY (sic) |
| `DAY_OF_YEAR` | 日 | 按年计算的日 | Calendar.DAY_OF_YEAR |
| `DAY_OF_MONTH` | 日 | 按月计算的日 | Calendar.DAY_OF_MONTH |
| `DAY_OF_WEEK` | 日 | 按周计算的日 | Calendar.DAY_OF_WEEK |
| `DAY_OF_WEEK_IN_MONTH` | 日 | 按当前月内的周计算的日 | Calendar.DAY_OF_WEEK_IN_MONTH |
| `HOUR_OF_DAY` | 时 | 按天计算的时 | Calendar.HOUR_OF_DAY |
| `MINUTE` | 分钟 | 分钟 | Calendar.MINUTE |
| `SECOND` | 秒 | 秒 | Calendar.SECOND |

**常用组合示例：**

| 需求 | periodTimeValue | periodTimeUnit |
|------|-----------------|----------------|
| 每 5 分钟 | 5 | `MINUTE` |
| 每 15 分钟 | 15 | `MINUTE` |
| 每小时 | 1 | `HOUR_OF_DAY` |
| 每 4 小时 | 4 | `HOUR_OF_DAY` |
| 每天 | 1 | `DAY_OF_YEAR` |
| 每周 | 1 | `DAY_OF_WEEK` |
| 每月 | 1 | `MONTH` |

---

## TriggerTimeAnchorEnum 枚举

```
包路径：pro.shushi.pamirs.trigger.enmu.TriggerTimeAnchorEnum
```

注意：包名是 `enmu`（框架原始拼写），不是 `enum`。

| 枚举值 | 显示名 | 说明 |
|--------|--------|------|
| `START` | 开始时 | 从任务**开始执行时**计算下一次周期。即使当前执行尚未完成，也按固定间隔触发下一次。适用于需要严格定时的场景。 |
| `FINISHED` | 完成时 | 从任务**执行完成后**计算下一次周期。确保两次执行之间有固定间隔。适用于耗时不确定的任务。 |

**绝大多数场景使用 `START`。**

---

## TaskType 枚举

```
包路径：pro.shushi.pamirs.middleware.schedule.eunmeration.TaskType
```

注意：包名是 `eunmeration`（框架原始拼写），不是 `enumeration`。

| 枚举值 | getValue() | 说明 |
|--------|------------|------|
| `CYCLE_SCHEDULE_NO_TRANSACTION_TASK` | `"CYCLE_SCHEDULE_NO_TRANSACTION_TASK"` | **最常用**。周期性任务，无事务控制 |
| `BASE_SCHEDULE_NO_TRANSACTION_TASK` | `"BASE_SCHEDULE_NO_TRANSACTION_TASK_KEY"` | 单次异步任务，无事务控制 |
| `BASE_SCHEDULE_TASK` | `"BASE_SCHEDULE_TASK"` | 基础任务，有事务控制 |
| `REMOTE_SCHEDULE_TASK` | `"REMOTE_SCHEDULE_TASK"` | 远程调度任务 |
| `SERIAL_BASE_SCHEDULE_NO_TRANSACTION_TASK` | - | 按 bizId 分片，无事务 |

**使用方式：** 总是通过 `.getValue()` 获取字符串值：
```java
scheduleTaskAction.setTaskType(TaskType.CYCLE_SCHEDULE_NO_TRANSACTION_TASK.getValue());
```

---

## ScheduleTaskActionService 接口

```
包路径：pro.shushi.pamirs.trigger.service.ScheduleTaskActionService
```

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `submit(ScheduleTaskAction)` | ScheduleTaskAction | Boolean | 提交/更新定时任务（幂等） |
| `delete(String technicalName)` | technicalName | Boolean | 永久删除任务 |
| `active(String technicalName)` | technicalName | Boolean | 激活任务 |
| `cancel(String technicalName)` | technicalName | Boolean | 停用任务 |
| `countByEntity(ScheduleTaskAction)` | ScheduleTaskAction | Long | 按条件统计任务数量 |

**注入方式：**
```java
@Autowired
private ScheduleTaskActionService scheduleTaskActionService;
```

**备选获取方式（在静态方法中使用）：**
```java
CommonApiFactory.getApi(ScheduleTaskActionService.class).submit(taskAction);
// 或
BeanDefinitionUtils.getBean(ScheduleTaskActionService.class).submit(taskAction);
```

---

## InstallDataInit / UpgradeDataInit 接口

```
包路径：
  pro.shushi.pamirs.boot.common.api.init.InstallDataInit
  pro.shushi.pamirs.boot.common.api.init.UpgradeDataInit
```

### InstallDataInit

```java
public interface InstallDataInit {
    /** 应用安装时调用 */
    boolean init(AppLifecycleCommand command, String version);
    /** 返回关联的模块标识列表 */
    List<String> modules();
    /** 初始化优先级，0 为默认 */
    int priority();
}
```

### UpgradeDataInit

```java
public interface UpgradeDataInit {
    /** 应用升级时调用 */
    boolean upgrade(AppLifecycleCommand command, String version, String existVersion);
    /** 返回关联的模块标识列表 */
    List<String> modules();
    /** 初始化优先级 */
    int priority();
}
```

**典型模式：** `upgrade()` 委托给 `init()`：
```java
@Override
public boolean upgrade(AppLifecycleCommand command, String version, String existVersion) {
    return init(command, version);
}
```

---

## Import 列表

### Task 实现类 import

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.domain.fun.FunctionDefinition;
import pro.shushi.pamirs.middleware.schedule.api.ScheduleAction;
import pro.shushi.pamirs.middleware.schedule.common.Result;
import pro.shushi.pamirs.middleware.schedule.domain.ScheduleItem;
import pro.shushi.pamirs.middleware.schedule.eunmeration.TaskType;
import pro.shushi.pamirs.trigger.enmu.TriggerTimeAnchorEnum;
import pro.shushi.pamirs.trigger.model.ScheduleTaskAction;
import pro.shushi.pamirs.trigger.service.ScheduleTaskActionService;
```

### Init 类 import

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.boot.common.api.command.AppLifecycleCommand;
import pro.shushi.pamirs.boot.common.api.init.InstallDataInit;
import pro.shushi.pamirs.boot.common.api.init.UpgradeDataInit;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

import java.util.Arrays;
import java.util.List;
```

### 可选 import（按需添加）

```java
// JSON 序列化（context 数据）
import pro.shushi.pamirs.meta.util.JsonUtils;

// 时间日历（firstExecuteTime 计算）
import java.util.Calendar;
import java.util.TimeZone;

// 静态方法中获取 service
import pro.shushi.pamirs.meta.common.spi.factory.CommonApiFactory;
import pro.shushi.pamirs.boot.base.utils.BeanDefinitionUtils;

// 集合工具
import com.google.common.collect.Lists;  // 用于 modules() 返回值
```

---

## FUN_NAMESPACE 命名惯例

**格式：** `"[项目标识].[模块].[可选子包].类名"`

**当前项目中已有的命名模式：**

| 项目 | 模块 | 示例 |
|------|------|------|
| deli-b2b | trade | `"deli.aries.schedule.DeliAriesAutoSignInTask"` |
| deli-b2b | trade | `"deli.trade.schedule.DeliAriesTradeEvaluationStatisticsTask"` |
| deli-b2b | item | `"deli.aries.item.schedule.DeliAriesItemAuthScheduleAction"` |
| deli-b2b | major/eip | `"deli.aries.major.DeliAriesSyncSalesOfficeSchedule"` |
| deli-b2b | inventory | `"escort.inventory.schedule.EscortSyncInventoryTimingSchedule"` |
| deli-b2b | fund | `"deli.fund.schedule.DeliAriesFundBookCreateSchedule"` |
| kailas | settlement | 与 `AriesRebateSettlementManager.FUN_NAMESPACE` 共用 |
| kailas | item | `"aries.item.schedule.AriesItemAuthScheduleAction"` |

**推荐默认格式：** `"deli.aries.[module].schedule.[ClassName]"`

---

## technicalName 命名惯例

项目中存在三种 technicalName 模式：

| 模式 | 格式 | 适用场景 |
|------|------|----------|
| 简洁版 | `FUN_NAMESPACE` | 周期任务，一个类对应一个任务 |
| 带方法 | `FUN_NAMESPACE + "#" + METHOD_NAME` | 最常见，推荐默认使用 |
| 带类名 | `FUN_NAMESPACE + "#" + ClassName + "#" + METHOD_NAME` | 区分度更高 |
| 动态版 | `FUN_NAMESPACE + "#" + timestamp + "#" + METHOD_NAME` | 动态创建的单次任务 |

**推荐默认使用带方法的格式。**

---

## firstExecuteTime 辅助方法

### 模式 A：明天凌晨指定小时

```java
private static Long getTomorrowStartTime() {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 1);  // 凌晨 1 点
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTimeInMillis();
}
```

### 模式 B：今天指定小时

```java
private static Long getTodayStartTime() {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
    calendar.set(Calendar.HOUR_OF_DAY, 3);  // 凌晨 3 点
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    return calendar.getTimeInMillis();
}
```

### 模式 C：立即执行

```java
scheduleTaskAction.setFirstExecuteTime(System.currentTimeMillis());
```

### 模式 D：延迟 N 分钟后执行

```java
scheduleTaskAction.setFirstExecuteTime(System.currentTimeMillis() + 1000L * 60 * N);
```
