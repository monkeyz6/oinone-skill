# Oinone 异步任务代码模板与示例

## 目录

1. [@XAsync 示例 1: 最简异步（无重试）](#xasync-示例-1-最简异步无重试)
2. [@XAsync 示例 2: 标准重试配置](#xasync-示例-2-标准重试配置)
3. [@XAsync 示例 3: 带延迟执行](#xasync-示例-3-带延迟执行)
4. [ExecuteTaskAction 示例 1: 独立任务类（简单 context）](#executetaskaction-示例-1-独立任务类简单-context)
5. [ExecuteTaskAction 示例 2: 三件套（Task定义 + Interface + Impl）](#executetaskaction-示例-2-三件套task定义--interface--impl)
6. [ExecuteTaskAction 示例 3: Manager 分离模式](#executetaskaction-示例-3-manager-分离模式)
7. [@XAsync 骨架模板](#xasync-骨架模板)
8. [ExecuteTaskAction 骨架模板](#executetaskaction-骨架模板)
9. [常见陷阱与注意事项](#常见陷阱与注意事项)

---

## @XAsync 示例 1: 最简异步（无重试）

> 场景：异步处理标签任务，不需要重试，失败即放弃。
> 参考：`DeliAriesItemTplServiceImpl.java`

### Interface

```java
package pro.shushi.deli.aries.item.api.api;

import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Fun(DeliAriesItemTplService.FUN_NAMESPACE)
public interface DeliAriesItemTplService {
    String FUN_NAMESPACE = "deli.aries.item.DeliAriesItemTplService";

    @Function
    void processTagAsyncTask(Long tagId);
}
```

### Impl

```java
package pro.shushi.deli.aries.item.core.service;

import org.springframework.stereotype.Service;
import pro.shushi.deli.aries.item.api.api.DeliAriesItemTplService;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.trigger.annotation.XAsync;

@Fun(DeliAriesItemTplService.FUN_NAMESPACE)
@Slf4j
@Service
public class DeliAriesItemTplServiceImpl implements DeliAriesItemTplService {

    @Function
    @Override
    @XAsync(displayName = "异步处理标签任务")
    public void processTagAsyncTask(Long tagId) {
        log.info("开始异步处理标签任务, tagId={}", tagId);
        // 业务逻辑
        log.info("完成异步处理标签任务, tagId={}", tagId);
    }
}
```

---

## @XAsync 示例 2: 标准重试配置

> 场景：经销商审核通过后，异步创建合作关系。失败需重试，最多3次，每次间隔60秒。
> 参考：`DeliAriesDistributorServiceImpl.java`

### Interface

```java
package pro.shushi.deli.aries.major.api.api;

import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Fun(DeliAriesDistributorService.FUN_NAMESPACE)
public interface DeliAriesDistributorService {
    String FUN_NAMESPACE = "deli.aries.major.DeliAriesDistributorService";

    @Function
    void asyncCreateCooperationAfterAudit(Long distributorId, Long partnerId);

    @Function
    void asyncCreateUserAfterAudit(Long distributorId, Long partnerId);
}
```

### Impl

```java
package pro.shushi.deli.aries.major.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.shushi.deli.aries.major.api.api.DeliAriesDistributorService;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.trigger.annotation.XAsync;

@Fun(DeliAriesDistributorService.FUN_NAMESPACE)
@Slf4j
@Service
public class DeliAriesDistributorServiceImpl implements DeliAriesDistributorService {

    // limitRetryNumber=3: 最多重试3次
    // nextRetryTimeValue=60: 每次重试间隔60秒
    // nextRetryTimeUnit 默认为 SECOND，无需显式指定
    @Function
    @Override
    @XAsync(displayName = "经销商审核通过,异步创建合作关系", limitRetryNumber = 3, nextRetryTimeValue = 60)
    public void asyncCreateCooperationAfterAudit(Long distributorId, Long partnerId) {
        log.info("开始异步创建合作关系, distributorId={}, partnerId={}", distributorId, partnerId);
        // 业务逻辑
        log.info("完成异步创建合作关系");
    }

    @Function
    @Override
    @XAsync(displayName = "经销商审核通过,异步创建员工", limitRetryNumber = 3, nextRetryTimeValue = 60)
    public void asyncCreateUserAfterAudit(Long distributorId, Long partnerId) {
        log.info("开始异步创建员工, distributorId={}", distributorId);
        // 业务逻辑
        log.info("完成异步创建员工");
    }
}
```

---

## @XAsync 示例 3: 带延迟执行

> 场景1：自动解单，延迟30秒执行（避免并发冲突）。
> 场景2：异步调用SAP清账接口，延迟20分钟执行（等待外部系统处理）。
> 参考：`DeliAriesTmsOrderServiceImpl.java`、`DeliAriesSapIntegrationServiceImpl.java`

### 短延迟（30秒）

```java
// delayTime=30: 延迟30秒后执行
// delayTimeUnit 默认为 SECOND，无需显式指定
@Function
@Override
@XAsync(displayName = "自动解单", limitRetryNumber = 3, delayTime = 30)
public void autoUnbind(Long orderId) {
    log.info("开始自动解单, orderId={}", orderId);
    // 业务逻辑
}
```

### 长延迟（20分钟）

```java
// delayTime=20, delayTimeUnit=MINUTE: 延迟20分钟后执行
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;

@Function
@Override
@XAsync(displayName = "异步调用sap清账接口", limitRetryNumber = 6, delayTime = 20, delayTimeUnit = TimeUnitEnum.MINUTE)
public void asyncCallSapClearAccount(Long recordId) {
    log.info("开始调用SAP清账接口, recordId={}", recordId);
    // 业务逻辑
}
```

---

## ExecuteTaskAction 示例 1: 独立任务类（简单 context）

> 场景：生成销售订单后，异步确认回调。Context 只传递一个 ID。
> 参考：`DeliAriesOutTradeOrderAckTask.java`

```java
package pro.shushi.deli.aries.trade.core.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.deli.aries.trade.api.api.DeliAriesOutTradeOrderService;
import pro.shushi.kailas.aries.common.enums.AriesCommonExpEnumerate;
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.common.exception.PamirsException;
import pro.shushi.pamirs.middleware.schedule.api.ScheduleAction;
import pro.shushi.pamirs.middleware.schedule.common.Result;
import pro.shushi.pamirs.middleware.schedule.domain.ScheduleItem;
import pro.shushi.pamirs.middleware.schedule.eunmeration.TaskType;
import pro.shushi.pamirs.trigger.model.ExecuteTaskAction;
import pro.shushi.pamirs.trigger.service.ExecuteTaskActionService;

import java.util.Date;

/**
 * 生成销售订单后，确认回调
 */
@Slf4j
@Component
@Fun(DeliAriesOutTradeOrderAckTask.FUN_NAMESPACE)
public class DeliAriesOutTradeOrderAckTask implements ScheduleAction {
    // FUN_NAMESPACE: 注册到 Pamirs 函数注册表的唯一标识
    public static final String FUN_NAMESPACE = "aries.trade.schedule.DeliAriesOutTradeOrderAckTask";
    // METHOD_NAME: execute 方法名，与 setExecuteFun 对应
    public static final String METHOD_NAME = "execute";
    public static String TASK_TECHNICAL_NAME = "DeliAriesOutTradeOrderAckTask:";

    @Autowired
    private ExecuteTaskActionService executeTaskActionService;
    @Autowired
    private DeliAriesOutTradeOrderService deliAriesOutTradeOrderService;

    /**
     * 创建并提交异步任务
     */
    public void createTask(Long id) {
        ExecuteTaskAction taskAction = new ExecuteTaskAction()
                // 任务类型：远程调度任务
                .setTaskType(TaskType.REMOTE_SCHEDULE_TASK.getValue())
                // 业务 ID：用于分片和关联查询
                .setBizId(id)
                // 重试配置：最多2次，间隔30秒
                .setLimitRetryNumber(2)
                .setNextRetryTimeValue(30)
                .setNextRetryTimeUnit(TimeUnitEnum.SECOND)
                // 显示名称
                .setDisplayName(TASK_TECHNICAL_NAME)
                // 执行函数：namespace + method，对应 ScheduleAction 的 getInterfaceName + execute
                .setExecuteNamespace(DeliAriesOutTradeOrderAckTask.FUN_NAMESPACE)
                .setExecuteFun(METHOD_NAME)
                .setActive(Boolean.TRUE)
                // 立即执行
                .setFirstExecuteTime(new Date().getTime())
                // Context：简单类型，直接 toString
                .setContext(id.toString())
                .construct();
        executeTaskActionService.submit(taskAction);
    }

    @Override
    @Function
    public Result<Void> execute(ScheduleItem scheduleItem) {
        Result<Void> result = new Result<>();
        result.setSuccess(true);
        // 简单类型反序列化
        Long id = Long.valueOf(scheduleItem.getContext());
        try {
            deliAriesOutTradeOrderService.pushOutTradeOrderAck(id);
        } catch (Exception e) {
            throw PamirsException.construct(AriesCommonExpEnumerate.EMPTY_ERROR)
                    .appendMsg(e.getMessage()).errThrow();
        }
        return result;
    }

    @Override
    public String getInterfaceName() {
        return FUN_NAMESPACE;
    }
}
```

---

## ExecuteTaskAction 示例 2: 三件套（Task定义 + Interface + Impl）

> 场景：平台优惠券活动状态变更，异步创建活动提报。需要传递复杂对象作为 Context。
> 参考：`DeliAriesPlatformCouponAsyncHandelTask.java` + `DeliAriesPlatformCouponAsyncHandelService.java` + `DeliAriesPlatformCouponAsyncHandelServiceImpl.java`

### Task 定义类

```java
package pro.shushi.deli.aries.promotion.core.task;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.middleware.schedule.core.tasks.BaseScheduleNoTransactionTask;

/**
 * 优惠券异步任务类型定义
 */
@Component
public class DeliAriesPlatformCouponAsyncHandelTask extends BaseScheduleNoTransactionTask {
    // TASK_TYPE: 任务类型标识，用于调度系统区分不同类型的任务
    public static final String TASK_TYPE = DeliAriesPlatformCouponAsyncHandelTask.class.getSimpleName();

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }
}
```

### Interface

```java
package pro.shushi.deli.aries.promotion.api.service.task;

import pro.shushi.deli.aries.promotion.api.tmodel.DeliPromotionDealStatusTransient;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Fun(DeliAriesPlatformCouponAsyncHandelService.FUN_NAMESPACE)
public interface DeliAriesPlatformCouponAsyncHandelService {
    String FUN_NAMESPACE = "deli.aries.promotion.DeliAriesActivityReportAsyncHandelService";

    /**
     * 平台活动创建活动提报任务
     */
    @Function
    void changePlatformCouponStatus(DeliPromotionDealStatusTransient dealStatusTransient);
}
```

### Impl（实现双接口：业务接口 + ScheduleAction）

```java
package pro.shushi.deli.aries.promotion.core.task.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.deli.aries.promotion.api.model.DeliActivity;
import pro.shushi.deli.aries.promotion.api.service.DeliActivityService;
import pro.shushi.deli.aries.promotion.api.service.DeliAriesActivityReportService;
import pro.shushi.deli.aries.promotion.api.service.task.DeliAriesPlatformCouponAsyncHandelService;
import pro.shushi.deli.aries.promotion.api.tmodel.DeliPromotionDealStatusTransient;
import pro.shushi.deli.aries.promotion.core.task.DeliAriesPlatformCouponAsyncHandelTask;
import pro.shushi.himalaya.promotion.enmu.act.ActStatusEnum;
import pro.shushi.himalaya.promotion.enmu.rule.RuleStatusEnum;
import pro.shushi.himalaya.promotion.model.activity.Activity;
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.util.JsonUtils;
import pro.shushi.pamirs.middleware.schedule.api.ScheduleAction;
import pro.shushi.pamirs.middleware.schedule.common.Result;
import pro.shushi.pamirs.middleware.schedule.domain.ScheduleItem;
import pro.shushi.pamirs.trigger.model.ExecuteTaskAction;
import pro.shushi.pamirs.trigger.service.ExecuteTaskActionService;

@Fun(DeliAriesPlatformCouponAsyncHandelService.FUN_NAMESPACE)
@Component
@Slf4j
public class DeliAriesPlatformCouponAsyncHandelServiceImpl
        implements DeliAriesPlatformCouponAsyncHandelService, ScheduleAction {

    @Autowired
    private ExecuteTaskActionService executeTaskActionService;
    @Autowired
    private DeliActivityService deliActivityService;
    @Autowired
    private DeliAriesActivityReportService deliAriesActivityReportService;

    @Override
    public String getInterfaceName() {
        return DeliAriesPlatformCouponAsyncHandelService.FUN_NAMESPACE;
    }

    /**
     * 任务执行逻辑：从 context 反序列化对象，执行业务处理
     */
    @Override
    public Result<Void> execute(ScheduleItem scheduleItem) {
        Result<Void> result = new Result<>();
        // JSON 反序列化获取上下文
        DeliPromotionDealStatusTransient statusTransient =
                JsonUtils.parseObject(scheduleItem.getContext(), DeliPromotionDealStatusTransient.class);
        DeliActivity activity = statusTransient.getActivity();
        RuleStatusEnum status = statusTransient.getStatus();

        // 业务校验
        DeliActivity dbAct = deliActivityService.queryById(activity.getId());
        if (RuleStatusEnum.PROCESSING.equals(status) && !ActStatusEnum.PREPARE.equals(dbAct.getStatus())) {
            return result;
        }
        if (RuleStatusEnum.FINISH.equals(status) && !ActStatusEnum.PROCESSING.equals(dbAct.getStatus())) {
            return result;
        }

        // 执行业务逻辑
        deliAriesActivityReportService.create(dbAct);
        return result;
    }

    /**
     * 构建并提交异步任务
     */
    @Override
    public void changePlatformCouponStatus(DeliPromotionDealStatusTransient dealStatusTransient) {
        Activity activity = dealStatusTransient.getActivity();
        executeTaskActionService.submit((ExecuteTaskAction) new ExecuteTaskAction()
                // 业务 ID：按活动 ID 分组
                .setBizId(activity.getId())
                // 任务类型：引用 Task 定义类的 TASK_TYPE
                .setTaskType(DeliAriesPlatformCouponAsyncHandelTask.TASK_TYPE)
                // 重试配置：最多1次，间隔5秒
                .setNextRetryTimeUnit(TimeUnitEnum.SECOND)
                .setNextRetryTimeValue(5)
                .setLimitRetryNumber(1)
                .setDisplayName("平台优惠券定时创建活动提报")
                // 指定执行时间（业务传入）
                .setFirstExecuteTime(dealStatusTransient.getDealTime())
                // 执行函数
                .setExecuteNamespace(getInterfaceName())
                .setExecuteFun("execute")
                // Context：复杂对象 JSON 序列化
                .setContext(JsonUtils.toJSONString(dealStatusTransient))
        );
    }
}
```

---

## ExecuteTaskAction 示例 3: Manager 分离模式

> 场景：业绩返利结算计算任务。任务提交逻辑在 Manager 中，执行逻辑在 TaskAction 中。
> 参考：`AriesRebateSettlementManager.java` + `AriesRebateSettlementTaskAction.java` + `AriesComputeRebateSettlementTask.java`

### Task 定义类

```java
package pro.shushi.kailas.aries.settlement.core.task;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.middleware.schedule.core.tasks.BaseScheduleNoTransactionTask;

@Component("ariesComputeRebateSettlementTask")
public class AriesComputeRebateSettlementTask extends BaseScheduleNoTransactionTask {
    // 自定义 TASK_TYPE 字符串（非 class.getSimpleName()）
    public static final String TASK_TYPE = "ARIES_REBATE_SETTLEMENT_SCHEDULE_TASK";

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }
}
```

### ScheduleAction 实现类（仅负责执行）

```java
package pro.shushi.kailas.aries.settlement.core.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.kailas.aries.settlement.api.tmodel.AriesRebateSettlementTaskRequest;
import pro.shushi.kailas.aries.settlement.core.manager.AriesRebateSettlementManager;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.common.exception.PamirsException;
import pro.shushi.pamirs.meta.util.JsonUtils;
import pro.shushi.pamirs.middleware.schedule.api.ScheduleAction;
import pro.shushi.pamirs.middleware.schedule.common.Result;
import pro.shushi.pamirs.middleware.schedule.domain.ScheduleItem;

@Slf4j
@Component
@Fun(AriesRebateSettlementManager.FUN_NAMESPACE)
public class AriesRebateSettlementTaskAction implements ScheduleAction {

    @Autowired
    private AriesRebateSettlementManager ariesRebateSettlementManager;

    @Override
    public String getInterfaceName() {
        return AriesRebateSettlementManager.FUN_NAMESPACE;
    }

    @Override
    public String getMethodName() {
        return AriesRebateSettlementManager.METHOD_NAME;
    }

    @Override
    public Result<Void> execute(ScheduleItem scheduleItem) {
        Result<Void> result = new Result<>();
        try {
            // JSON 反序列化
            AriesRebateSettlementTaskRequest request =
                    JsonUtils.parseObject(scheduleItem.getContext(), AriesRebateSettlementTaskRequest.class);
            log.info("开始业绩结算单的计算，{}", JsonUtils.toJSONString(request));
            ariesRebateSettlementManager.computeRebateSettlement(request);
            log.info("结束业绩结算单的计算，{}", request.getComputeTask().getCode());
        } catch (PamirsException e) {
            log.error("AriesRebateSettlementManager execute error.", e);
            result.setFail(e.getMessage());
        } catch (Exception e) {
            log.error("AriesRebateSettlementManager execute error.", e);
            result.setFail(e.getMessage());
        }
        return result;
    }
}
```

### Manager（负责任务提交）

```java
package pro.shushi.kailas.aries.settlement.core.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.kailas.aries.settlement.api.tmodel.AriesRebateSettlementTaskRequest;
import pro.shushi.kailas.aries.settlement.core.task.AriesComputeRebateSettlementTask;
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.util.JsonUtils;
import pro.shushi.pamirs.trigger.model.ExecuteTaskAction;
import pro.shushi.pamirs.trigger.service.ExecuteTaskActionService;

@Slf4j
@Component
@Fun(AriesRebateSettlementManager.FUN_NAMESPACE)
public class AriesRebateSettlementManager {

    public static final String TASK_TYPE = AriesComputeRebateSettlementTask.TASK_TYPE;
    public static final String FUN_NAMESPACE = "aries.settlement.schedule.AriesRebateSettlementManager";
    public static final String METHOD_NAME = "execute";

    @Autowired
    private ExecuteTaskActionService executeTaskActionService;

    /**
     * 提交结算计算任务
     */
    private Boolean submitComputeRebateSettlementTask(
            AriesRebateComputeTask task, AriesRebateComputeTaskDetail taskDetail) {
        AriesRebateSettlementTaskRequest request = new AriesRebateSettlementTaskRequest()
                .setComputeTask(task)
                .setTaskDetail(taskDetail);

        return executeTaskActionService.submit((ExecuteTaskAction) new ExecuteTaskAction()
                // 使用 bizCode 分组（按任务编码串行执行）
                .setBizCode(task.getCode())
                .setTaskType(TASK_TYPE)
                // 重试配置：最多3次，间隔90秒
                .setNextRetryTimeUnit(TimeUnitEnum.SECOND)
                .setNextRetryTimeValue(90)
                .setLimitRetryNumber(3)
                .setDisplayName("ARIES业绩返利结算计算任务")
                .setExecuteNamespace(FUN_NAMESPACE)
                .setExecuteFun(METHOD_NAME)
                // Context：复杂对象 JSON 序列化
                .setContext(JsonUtils.toJSONString(request))
                .construct());
    }
}
```

---

## @XAsync 骨架模板

当用户只提供了模糊需求时，使用以下骨架模板。

### Interface 骨架

```java
package pro.shushi.{deli/kailas}.aries.{module}.api.api;

import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Fun({ServiceName}.FUN_NAMESPACE)
public interface {ServiceName} {
    String FUN_NAMESPACE = "{项目}.{模块}.{ServiceName}";

    @Function
    void async{BusinessName}({参数类型} {参数名});
}
```

### Impl 骨架

```java
package pro.shushi.{deli/kailas}.aries.{module}.core.service;

import org.springframework.stereotype.Service;
import pro.shushi.{deli/kailas}.aries.{module}.api.api.{ServiceName};
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.trigger.annotation.XAsync;

@Fun({ServiceName}.FUN_NAMESPACE)
@Slf4j
@Service
public class {ServiceName}Impl implements {ServiceName} {

    // TODO: 注入需要的业务 Service

    @Function
    @Override
    @XAsync(displayName = "{中文描述}", limitRetryNumber = 3, nextRetryTimeValue = 60)
    public void async{BusinessName}({参数类型} {参数名}) {
        log.info("开始-{中文描述}, {}={}", "{参数名}", {参数名});
        // TODO: 实现业务逻辑
        log.info("完成-{中文描述}");
    }
}
```

---

## ExecuteTaskAction 骨架模板

### Task 定义类骨架（如需自定义任务类型）

```java
package pro.shushi.{deli/kailas}.aries.{module}.core.task;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.middleware.schedule.core.tasks.BaseScheduleNoTransactionTask;

@Component
public class {TaskClassName} extends BaseScheduleNoTransactionTask {
    public static final String TASK_TYPE = {TaskClassName}.class.getSimpleName();

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }
}
```

### ScheduleAction 实现类骨架

```java
package pro.shushi.{deli/kailas}.aries.{module}.core.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.util.JsonUtils;
import pro.shushi.pamirs.middleware.schedule.api.ScheduleAction;
import pro.shushi.pamirs.middleware.schedule.common.Result;
import pro.shushi.pamirs.middleware.schedule.domain.ScheduleItem;
import pro.shushi.pamirs.middleware.schedule.eunmeration.TaskType;
import pro.shushi.pamirs.trigger.model.ExecuteTaskAction;
import pro.shushi.pamirs.trigger.service.ExecuteTaskActionService;

@Slf4j
@Component
@Fun({ClassName}.FUN_NAMESPACE)
public class {ClassName} implements ScheduleAction {
    public static final String FUN_NAMESPACE = "{项目}.{模块}.schedule.{ClassName}";
    public static final String METHOD_NAME = "execute";

    @Autowired
    private ExecuteTaskActionService executeTaskActionService;
    // TODO: 注入需要的业务 Service

    /**
     * 创建并提交异步任务
     */
    public void createTask({参数类型} {参数名}) {
        ExecuteTaskAction taskAction = new ExecuteTaskAction()
                .setTaskType(TaskType.BASE_SCHEDULE_NO_TRANSACTION_TASK.getValue())  // TODO: 根据需要调整
                .setBizId({bizId})                                                    // TODO: 设置业务ID
                .setLimitRetryNumber(3)                                               // TODO: 调整重试次数
                .setNextRetryTimeValue(60)
                .setNextRetryTimeUnit(TimeUnitEnum.SECOND)
                .setDisplayName("{中文描述}")
                .setExecuteNamespace(FUN_NAMESPACE)
                .setExecuteFun(METHOD_NAME)
                .setActive(Boolean.TRUE)
                .setFirstExecuteTime(System.currentTimeMillis())
                .setContext(JsonUtils.toJSONString({参数名}))                          // TODO: 根据需要调整序列化方式
                .construct();
        executeTaskActionService.submit(taskAction);
    }

    @Override
    public String getInterfaceName() {
        return FUN_NAMESPACE;
    }

    @Override
    @Function
    public Result<Void> execute(ScheduleItem scheduleItem) {
        Result<Void> result = new Result<>();
        try {
            // TODO: 反序列化 context
            // {ContextType} data = JsonUtils.parseObject(scheduleItem.getContext(), {ContextType}.class);
            log.info("开始-{中文描述}");

            // TODO: 实现业务逻辑

            result.setSuccess(true);
            log.info("完成-{中文描述}");
        } catch (Exception e) {
            log.error("{中文描述}执行异常", e);
            result.setFail(e.getMessage());
        }
        return result;
    }
}
```

---

## 常见陷阱与注意事项

### 1. @XAsync AOP 代理自调用问题

`@XAsync` 基于 Spring AOP 代理实现。**同类内直接调用 `this.asyncMethod()` 不会触发异步**，必须通过注入的 Service 引用调用：

```java
// ❌ 错误：自调用不触发异步
public void someMethod() {
    this.asyncDoSomething();  // 同步执行！
}

// ✅ 正确：通过注入的 Service 调用
@Autowired
private MyService myService;  // 注入自身接口

public void someMethod() {
    myService.asyncDoSomething();  // 异步执行
}
```

### 2. @Slf4j 包路径

Oinone 框架使用自己的 `@Slf4j` 注解，**不是 Lombok 的**：

```java
// ✅ 正确
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

// ❌ 错误
import lombok.extern.slf4j.Slf4j;
```

### 3. 框架包名拼写

框架中有几个非标准拼写的包名，生成代码时必须使用正确的拼写：

```java
// 注意 enmu（不是 enum）
import pro.shushi.pamirs.core.common.enmu.TimeUnitEnum;

// 注意 eunmeration（不是 enumeration）
import pro.shushi.pamirs.middleware.schedule.eunmeration.TaskType;
```

### 4. construct() 方法

`construct()` 是可选的。项目中两种写法都有使用，不会影响功能。新代码建议使用（保持一致性）。

### 5. @Function 注解

`@XAsync` 方法**必须**同时加 `@Function` 注解，否则不会被 Pamirs 函数注册表识别：

```java
// ✅ 正确：@Function + @Override + @XAsync
@Function
@Override
@XAsync(displayName = "...")
public void asyncXxx() { }

// ❌ 错误：缺少 @Function
@Override
@XAsync(displayName = "...")
public void asyncXxx() { }
```

### 6. ExecuteTaskAction 的 execute 方法也需要 @Function

ScheduleAction 实现类的 `execute()` 方法需要加 `@Function` 注解：

```java
@Override
@Function  // 必须加
public Result<Void> execute(ScheduleItem scheduleItem) { }
```

### 7. Interface 中的异步方法声明

Interface 中的异步方法只需要 `@Function`，**不需要** `@XAsync`（`@XAsync` 只加在 Impl 上）：

```java
// Interface
@Function
void asyncXxx(Long id);  // 只需 @Function

// Impl
@Function
@Override
@XAsync(displayName = "...")  // @XAsync 只在 Impl 上
public void asyncXxx(Long id) { }
```
