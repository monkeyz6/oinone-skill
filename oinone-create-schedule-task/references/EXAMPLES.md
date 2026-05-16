# Oinone 定时任务代码示例

## 目录

1. [示例 1: 标准周期任务（每小时）](#示例-1-标准周期任务每小时)
2. [示例 2: 高频周期任务（每 15 分钟，含幂等检查）](#示例-2-高频周期任务每-15-分钟含幂等检查)
3. [示例 3: 每日定时任务（凌晨 3 点）](#示例-3-每日定时任务凌晨-3-点)
4. [示例 4: 单任务独立 Init](#示例-4-单任务独立-init)
5. [示例 5: 多任务合并 Init](#示例-5-多任务合并-init)
6. [示例 6: 动态创建单次任务（含 context）](#示例-6-动态创建单次任务含-context)
7. [骨架代码模板](#骨架代码模板快速开始)

---

## 示例 1: 标准周期任务（每小时）

> 场景：每小时执行一次自动签收逻辑，明天凌晨 1 点首次执行。
> 参考：`DeliAriesAutoSignInTask.java`

```java
package pro.shushi.deli.aries.trade.core.task;

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

import java.util.Calendar;

@Slf4j
@Component
@Fun(DeliAriesAutoSignInTask.FUN_NAMESPACE)
public class DeliAriesAutoSignInTask implements ScheduleAction {
    public static final String FUN_NAMESPACE = "deli.aries.schedule.DeliAriesAutoSignInTask";
    public static final String METHOD_NAME = "execute";

    @Autowired
    private ScheduleTaskActionService scheduleTaskActionService;

    /**
     * 初始化定时任务配置并提交到调度器
     */
    public void initTask() {
        log.info("定时任务[自动签收定时任务]初始化");
        ScheduleTaskAction scheduleTaskAction = new ScheduleTaskAction();
        scheduleTaskAction.setDisplayName("自动签收定时任务");
        scheduleTaskAction.setTechnicalName(DeliAriesAutoSignInTask.FUN_NAMESPACE);
        // 执行次数：-1 表示无限循环
        scheduleTaskAction.setLimitExecuteNumber(-1);
        // 周期：每 1 小时
        scheduleTaskAction.setPeriodTimeValue(1);
        scheduleTaskAction.setPeriodTimeUnit(TimeUnitEnum.HOUR_OF_DAY);
        scheduleTaskAction.setPeriodTimeAnchor(TriggerTimeAnchorEnum.START);
        // 重试：失败后重试 1 次，间隔 1 分钟
        scheduleTaskAction.setLimitRetryNumber(1);
        scheduleTaskAction.setNextRetryTimeValue(1);
        scheduleTaskAction.setNextRetryTimeUnit(TimeUnitEnum.MINUTE);
        // 执行函数
        scheduleTaskAction.setExecuteNamespace(DeliAriesAutoSignInTask.FUN_NAMESPACE);
        scheduleTaskAction.setExecuteFun(DeliAriesAutoSignInTask.METHOD_NAME);
        scheduleTaskAction.setExecuteFunction(new FunctionDefinition().setTimeout(5000));
        // 任务类型：周期性，无事务
        scheduleTaskAction.setTaskType(TaskType.CYCLE_SCHEDULE_NO_TRANSACTION_TASK.getValue());
        scheduleTaskAction.setContext(null);
        scheduleTaskAction.setActive(true);
        // 首次执行：明天凌晨 1 点
        scheduleTaskAction.setFirstExecuteTime(this.getTodayStartTime());
        scheduleTaskActionService.submit(scheduleTaskAction);
    }

    @Override
    public String getInterfaceName() {
        return DeliAriesAutoSignInTask.FUN_NAMESPACE;
    }

    @Override
    public String getMethodName() {
        return DeliAriesAutoSignInTask.METHOD_NAME;
    }

    @Override
    public Result<Void> execute(ScheduleItem scheduleItem) {
        log.info("开始-自动签收定时任务");
        Result<Void> result = new Result<>();
        // TODO: 业务逻辑
        result.setSuccess(true);
        log.info("完成-自动签收定时任务");
        return result;
    }

    /**
     * 明天凌晨 1 点
     */
    private static Long getTodayStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
```

---

## 示例 2: 高频周期任务（每 15 分钟，含幂等检查）

> 场景：每 15 分钟同步库存数据，立即首次执行，提交前检查是否已存在同名任务。
> 参考：`EscortSyncInventoryTimingSchedule.java`

```java
package pro.shushi.deli.aries.inventory.core.task.execute;

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

@Slf4j
@Component
@Fun(EscortSyncInventoryTimingSchedule.FUN_NAMESPACE)
public class EscortSyncInventoryTimingSchedule implements ScheduleAction {
    public static final String FUN_NAMESPACE = "escort.inventory.schedule.EscortSyncInventoryTimingSchedule";
    public static final String METHOD_NAME = "execute";

    @Autowired
    private ScheduleTaskActionService scheduleTaskActionService;

    public void initTask() {
        log.info("定时任务[同步库存]开始初始化");
        ScheduleTaskAction scheduleTaskAction = new ScheduleTaskAction();
        scheduleTaskAction.setDisplayName("同步库存");
        scheduleTaskAction.setTechnicalName(EscortSyncInventoryTimingSchedule.FUN_NAMESPACE + "#" + METHOD_NAME);
        scheduleTaskAction.setLimitExecuteNumber(-1);
        // 周期：每 15 分钟
        scheduleTaskAction.setPeriodTimeValue(15);
        scheduleTaskAction.setPeriodTimeUnit(TimeUnitEnum.MINUTE);
        scheduleTaskAction.setPeriodTimeAnchor(TriggerTimeAnchorEnum.START);
        scheduleTaskAction.setLimitRetryNumber(1);
        scheduleTaskAction.setNextRetryTimeValue(1);
        scheduleTaskAction.setNextRetryTimeUnit(TimeUnitEnum.MINUTE);
        scheduleTaskAction.setExecuteNamespace(EscortSyncInventoryTimingSchedule.FUN_NAMESPACE);
        scheduleTaskAction.setExecuteFun(EscortSyncInventoryTimingSchedule.METHOD_NAME);
        // 超时 50 秒（库存同步可能耗时较长）
        scheduleTaskAction.setExecuteFunction(new FunctionDefinition().setTimeout(50000));
        scheduleTaskAction.setTaskType(TaskType.CYCLE_SCHEDULE_NO_TRANSACTION_TASK.getValue());
        scheduleTaskAction.setContext("syncInventory");
        scheduleTaskAction.setActive(true);
        // 立即执行
        scheduleTaskAction.setFirstExecuteTime(System.currentTimeMillis());
        // 幂等检查：任务已存在则不重复创建
        Long count = scheduleTaskActionService.countByEntity(scheduleTaskAction);
        if (count == null || count == 0) {
            log.info("初始化同步库存任务");
            scheduleTaskActionService.submit(scheduleTaskAction);
        }
    }

    @Override
    public String getInterfaceName() {
        return EscortSyncInventoryTimingSchedule.FUN_NAMESPACE;
    }

    @Override
    public String getMethodName() {
        return EscortSyncInventoryTimingSchedule.METHOD_NAME;
    }

    @Override
    public Result<Void> execute(ScheduleItem scheduleItem) {
        log.info("开始-同步库存任务");
        Result<Void> result = new Result<>();
        String context = scheduleItem.getContext();
        // 根据 context 区分不同逻辑
        if ("syncInventory".equals(context)) {
            // TODO: 同步库存逻辑
        }
        result.setSuccess(true);
        log.info("完成-同步库存任务");
        return result;
    }
}
```

---

## 示例 3: 每日定时任务（凌晨 3 点）

> 场景：每天凌晨 3 点同步 SAP 数据，使用带类名的 technicalName 格式。
> 参考：`DeliAriesSyncSalesOfficeSchedule.java`

```java
package pro.shushi.deli.aries.eip.core.thirid.task.schedule;

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

import java.util.Calendar;
import java.util.TimeZone;

@Slf4j
@Component
@Fun(DeliAriesSyncSalesOfficeSchedule.FUN_NAMESPACE)
public class DeliAriesSyncSalesOfficeSchedule implements ScheduleAction {
    public static final String FUN_NAMESPACE = "deli.aries.major.DeliAriesSyncSalesOfficeSchedule";
    public static final String METHOD_NAME = "execute";

    @Autowired
    private ScheduleTaskActionService scheduleTaskActionService;

    public void initTask() {
        log.info("定时任务[同步Sap销售办公室]开始初始化");
        ScheduleTaskAction scheduleTaskAction = new ScheduleTaskAction();
        scheduleTaskAction.setDisplayName("同步Sap销售办公室");
        // technicalName 带类名和方法名，区分度更高
        scheduleTaskAction.setTechnicalName(FUN_NAMESPACE + "#"
                + DeliAriesSyncSalesOfficeSchedule.class.getSimpleName() + "#" + METHOD_NAME);
        scheduleTaskAction.setLimitExecuteNumber(-1);
        // 周期：每 1 天
        scheduleTaskAction.setPeriodTimeValue(1);
        scheduleTaskAction.setPeriodTimeUnit(TimeUnitEnum.DAY_OF_YEAR);
        scheduleTaskAction.setPeriodTimeAnchor(TriggerTimeAnchorEnum.START);
        scheduleTaskAction.setLimitRetryNumber(1);
        scheduleTaskAction.setNextRetryTimeValue(1);
        scheduleTaskAction.setNextRetryTimeUnit(TimeUnitEnum.MINUTE);
        scheduleTaskAction.setExecuteNamespace(FUN_NAMESPACE);
        scheduleTaskAction.setExecuteFun(METHOD_NAME);
        scheduleTaskAction.setExecuteFunction(new FunctionDefinition().setTimeout(5000));
        scheduleTaskAction.setTaskType(TaskType.CYCLE_SCHEDULE_NO_TRANSACTION_TASK.getValue());
        scheduleTaskAction.setContext(null);
        scheduleTaskAction.setActive(true);
        // 今天凌晨 3 点首次执行
        scheduleTaskAction.setFirstExecuteTime(getTodayStartTime());
        scheduleTaskActionService.submit(scheduleTaskAction);
    }

    @Override
    public String getInterfaceName() {
        return FUN_NAMESPACE;
    }

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public Result<Void> execute(ScheduleItem scheduleItem) {
        log.info("开始-同步Sap销售办公室");
        Result<Void> result = new Result<>();
        // TODO: 同步逻辑
        result.setSuccess(true);
        log.info("完成-同步Sap销售办公室");
        return result;
    }

    /**
     * 获取当天凌晨 3 点的时间戳（GMT+8）
     */
    public static long getTodayStartTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.HOUR_OF_DAY, 3);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }
}
```

---

## 示例 4: 单任务独立 Init

> 场景：为单个定时任务创建独立的 Init 初始化类。
> 参考：`DeliAriesAutoSignInTaskInit.java`

```java
package pro.shushi.deli.aries.trade.core.task.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.deli.aries.trade.api.DeliAriesTradeModule;
import pro.shushi.deli.aries.trade.core.task.DeliAriesAutoSignInTask;
import pro.shushi.pamirs.boot.common.api.command.AppLifecycleCommand;
import pro.shushi.pamirs.boot.common.api.init.InstallDataInit;
import pro.shushi.pamirs.boot.common.api.init.UpgradeDataInit;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class DeliAriesAutoSignInTaskInit implements InstallDataInit, UpgradeDataInit {

    @Autowired
    private DeliAriesAutoSignInTask deliAriesAutoSignInTask;

    @Override
    public boolean init(AppLifecycleCommand command, String version) {
        log.info("初始化自动签收定时任务");
        deliAriesAutoSignInTask.initTask();
        return Boolean.TRUE;
    }

    @Override
    public boolean upgrade(AppLifecycleCommand command, String version, String existVersion) {
        return init(command, version);
    }

    @Override
    public List<String> modules() {
        // 返回所属模块的 MODULE_MODULE 常量
        return Arrays.asList(DeliAriesTradeModule.MODULE_MODULE);
    }

    @Override
    public int priority() {
        return 0;
    }
}
```

---

## 示例 5: 多任务合并 Init

> 场景：在一个 Init 类中初始化多个定时任务，适合同一模块的多个调度任务。
> 参考：`DeliAriesEipScheduleInit.java`

```java
package pro.shushi.deli.aries.eip.core.thirid;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.deli.aries.eip.core.thirid.task.schedule.*;
import pro.shushi.deli.aries.major.api.DeliAriesMajorModule;
import pro.shushi.pamirs.boot.common.api.command.AppLifecycleCommand;
import pro.shushi.pamirs.boot.common.api.init.InstallDataInit;
import pro.shushi.pamirs.boot.common.api.init.UpgradeDataInit;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

import java.util.List;

@Slf4j
@Component
public class DeliAriesEipScheduleInit implements InstallDataInit, UpgradeDataInit {

    @Autowired
    private DeliAriesSyncSalesOfficeSchedule deliAriesSyncSalesOfficeSchedule;
    @Autowired
    private DeliAriesSyncSalesOrganizationSchedule deliAriesSyncSalesOrganizationSchedule;
    @Autowired
    private DeliAriesSyncWarehouseSchedule deliAriesSyncWarehouseSchedule;
    @Autowired
    private DeliAriesSyncInventorySchedule deliAriesSyncInventorySchedule;

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean init(AppLifecycleCommand command, String version) {
        this.initTasks();
        return true;
    }

    @Override
    public boolean upgrade(AppLifecycleCommand command, String version, String existVersion) {
        // upgrade 阶段是否重新初始化任务，根据业务需要决定
        // 若任务配置可能变更，可调用 initTasks()
        return true;
    }

    @Override
    public List<String> modules() {
        return Lists.newArrayList(DeliAriesMajorModule.MODULE_MODULE);
    }

    public void initTasks() {
        // 初始化同步销售办公室定时任务
        deliAriesSyncSalesOfficeSchedule.initTask();
        // 初始化同步销售机构
        deliAriesSyncSalesOrganizationSchedule.initTask();
        // 初始化同步仓库
        deliAriesSyncWarehouseSchedule.initTask();
        // 初始化可售库存同步
        deliAriesSyncInventorySchedule.initTask();
    }
}
```

---

## 示例 6: 动态创建单次任务（含 context）

> 场景：根据业务事件动态创建一次性执行的定时任务，携带 JSON 序列化的上下文数据。
> 参考：`DeliAriesTPLQueryDeliveryOutstockTask.java`

```java
package pro.shushi.deli.aries.eip.core.thirid.task;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.common.spi.factory.CommonApiFactory;
import pro.shushi.pamirs.meta.domain.fun.FunctionDefinition;
import pro.shushi.pamirs.meta.util.JsonUtils;
import pro.shushi.pamirs.middleware.schedule.api.ScheduleAction;
import pro.shushi.pamirs.middleware.schedule.common.Result;
import pro.shushi.pamirs.middleware.schedule.domain.ScheduleItem;
import pro.shushi.pamirs.middleware.schedule.eunmeration.TaskType;
import pro.shushi.pamirs.trigger.model.ScheduleTaskAction;
import pro.shushi.pamirs.trigger.service.ScheduleTaskActionService;

@Component
@Fun(DeliAriesTPLQueryDeliveryOutstockTask.FUN_NAMESPACE)
@Slf4j
public class DeliAriesTPLQueryDeliveryOutstockTask implements ScheduleAction {
    public static final String FUN_NAMESPACE = "deli.aries.eip.schedule.DeliAriesTPLQueryDeliveryOutstockTask";
    public static final String METHOD_NAME = "execute";

    /**
     * 动态创建单次任务（静态方法，可在任意业务代码中调用）
     *
     * @param content 包含 orderId、executeNum、nextExecuteTime 等信息
     */
    public static void createTask(TplQueryTaskContent content) {
        ScheduleTaskAction taskAction = new ScheduleTaskAction();
        // 动态 technicalName：确保每次创建唯一的任务
        taskAction.setTechnicalName("查询发货单" + content.getOrderId() + "-" + content.getExecuteNum());
        taskAction.setBizId(content.getOrderId());
        // 单次执行
        taskAction.setLimitExecuteNumber(1);
        taskAction.setPeriodTimeValue(0);
        taskAction.setTaskType(TaskType.BASE_SCHEDULE_NO_TRANSACTION_TASK.getValue());
        // 失败重试 6 次，间隔 300 秒
        taskAction.setLimitRetryNumber(6);
        taskAction.setNextRetryTimeValue(300);
        taskAction.setNextRetryTimeUnit(TimeUnitEnum.SECOND);
        // 执行函数
        taskAction.setExecuteNamespace(DeliAriesTPLQueryDeliveryOutstockTask.FUN_NAMESPACE);
        taskAction.setExecuteFun(DeliAriesTPLQueryDeliveryOutstockTask.METHOD_NAME);
        taskAction.setExecuteFunction(new FunctionDefinition().setTimeout(60000));
        taskAction.setActive(Boolean.TRUE);
        // 首次执行时间由业务决定
        taskAction.setFirstExecuteTime(content.getNextExecuteTime().getTime());
        // 通过 JSON 传递上下文数据
        taskAction.setContext(JsonUtils.toJSONString(content));
        // 静态方法中通过 CommonApiFactory 获取 service
        CommonApiFactory.getApi(ScheduleTaskActionService.class).submit(taskAction);
    }

    @Override
    public String getInterfaceName() {
        return FUN_NAMESPACE;
    }

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public Result<Void> execute(ScheduleItem scheduleItem) {
        log.info("开始-查询发货单出库状态");
        Result<Void> result = new Result<>();
        try {
            // 从 context 反序列化业务数据
            String context = scheduleItem.getContext();
            TplQueryTaskContent content = JsonUtils.parseObject(context, TplQueryTaskContent.class);
            // TODO: 查询发货单逻辑
            result.setSuccess(true);
        } catch (Exception e) {
            log.error("查询发货单出库状态异常", e);
            result.setFail(e.getMessage());
        }
        log.info("完成-查询发货单出库状态");
        return result;
    }
}
```

> **注意**：动态创建的单次任务通常不需要 Init 类（不需要启动时自动注册），而是在业务代码中按需调用 `createTask()` 方法。

---

## 骨架代码模板（快速开始）

当用户只提供了模糊的需求描述时，使用以下骨架模板，将关键逻辑以 TODO 注释标注：

### Task 类骨架

```java
package pro.shushi.deli.aries.{module}.core.task;

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

@Slf4j
@Component
@Fun({ClassName}.FUN_NAMESPACE)
public class {ClassName} implements ScheduleAction {
    public static final String FUN_NAMESPACE = "deli.aries.{module}.schedule.{ClassName}";
    public static final String METHOD_NAME = "execute";

    @Autowired
    private ScheduleTaskActionService scheduleTaskActionService;
    // TODO: 注入需要的业务 Service

    public void initTask() {
        log.info("定时任务[{displayName}]初始化");
        ScheduleTaskAction scheduleTaskAction = new ScheduleTaskAction();
        scheduleTaskAction.setDisplayName("{displayName}");
        scheduleTaskAction.setTechnicalName({ClassName}.FUN_NAMESPACE + "#" + METHOD_NAME);
        scheduleTaskAction.setLimitExecuteNumber(-1);
        scheduleTaskAction.setPeriodTimeValue(1);                        // TODO: 调整周期数值
        scheduleTaskAction.setPeriodTimeUnit(TimeUnitEnum.HOUR_OF_DAY);  // TODO: 调整周期单位
        scheduleTaskAction.setPeriodTimeAnchor(TriggerTimeAnchorEnum.START);
        scheduleTaskAction.setLimitRetryNumber(1);
        scheduleTaskAction.setNextRetryTimeValue(1);
        scheduleTaskAction.setNextRetryTimeUnit(TimeUnitEnum.MINUTE);
        scheduleTaskAction.setExecuteNamespace({ClassName}.FUN_NAMESPACE);
        scheduleTaskAction.setExecuteFun({ClassName}.METHOD_NAME);
        scheduleTaskAction.setExecuteFunction(new FunctionDefinition().setTimeout(5000));  // TODO: 调整超时
        scheduleTaskAction.setTaskType(TaskType.CYCLE_SCHEDULE_NO_TRANSACTION_TASK.getValue());
        scheduleTaskAction.setContext(null);
        scheduleTaskAction.setActive(true);
        scheduleTaskAction.setFirstExecuteTime(System.currentTimeMillis());  // TODO: 调整首次执行时间
        scheduleTaskActionService.submit(scheduleTaskAction);
    }

    @Override
    public String getInterfaceName() {
        return {ClassName}.FUN_NAMESPACE;
    }

    @Override
    public String getMethodName() {
        return {ClassName}.METHOD_NAME;
    }

    @Override
    public Result<Void> execute(ScheduleItem scheduleItem) {
        log.info("开始-{displayName}");
        Result<Void> result = new Result<>();
        try {
            // TODO: 实现业务逻辑
            result.setSuccess(true);
        } catch (Exception e) {
            log.error("{displayName}执行异常", e);
            result.setFail(e.getMessage());
        }
        log.info("完成-{displayName}");
        return result;
    }
}
```

### Init 类骨架

```java
package pro.shushi.deli.aries.{module}.core.task.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.deli.aries.{module}.api.{ModuleClass};  // TODO: 替换为实际模块类
import pro.shushi.deli.aries.{module}.core.task.{ClassName};
import pro.shushi.pamirs.boot.common.api.command.AppLifecycleCommand;
import pro.shushi.pamirs.boot.common.api.init.InstallDataInit;
import pro.shushi.pamirs.boot.common.api.init.UpgradeDataInit;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class {ClassName}Init implements InstallDataInit, UpgradeDataInit {

    @Autowired
    private {ClassName} {fieldName};

    @Override
    public boolean init(AppLifecycleCommand command, String version) {
        log.info("初始化{displayName}");
        {fieldName}.initTask();
        return Boolean.TRUE;
    }

    @Override
    public boolean upgrade(AppLifecycleCommand command, String version, String existVersion) {
        return init(command, version);
    }

    @Override
    public List<String> modules() {
        return Arrays.asList({ModuleClass}.MODULE_MODULE);  // TODO: 替换为实际模块常量
    }

    @Override
    public int priority() {
        return 0;
    }
}
```
