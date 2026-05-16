# Oinone 触发任务 代码模板与示例

## ON_CREATE 触发 — 数据创建时

### 基本示例

```java
package pro.shushi.deli.aries.trade.core.trigger;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;
import pro.shushi.pamirs.trigger.annotation.Trigger;
import pro.shushi.pamirs.trigger.enmu.TriggerConditionEnum;

/**
 * 订单创建触发任务
 * 当订单数据被创建时，自动触发后续处理逻辑
 */
@Slf4j
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderTrigger {

    @Trigger(displayName = "订单创建后触发",
             name = DemoOrder.MODEL_MODEL + "#Trigger#onCreate",
             condition = TriggerConditionEnum.ON_CREATE)
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoOrder onCreate(DemoOrder data) {
        log.info("订单创建触发任务执行，订单ID: {}", data.getId());
        // TODO: 在此编写创建后的业务逻辑
        // 例如：发送通知、同步数据到外部系统、初始化关联数据等
        return data;
    }
}
```

### 带事件追踪的 ON_CREATE

```java
@Slf4j
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderTrigger {

    @Trigger(displayName = "订单创建后触发（带事件追踪）",
             name = DemoOrder.MODEL_MODEL + "#Trigger#onCreateWithEvent",
             condition = TriggerConditionEnum.ON_CREATE,
             eventParameter = "msgId")
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoOrder onCreateWithEvent(DemoOrder data, String msgId) {
        log.info("订单创建触发任务执行，订单ID: {}, 事件ID: {}", data.getId(), msgId);
        // msgId 可用于消息去重和事件追踪
        return data;
    }
}
```

## ON_UPDATE 触发 — 数据更新时

### 基本示例（before + after 双参数）

```java
package pro.shushi.deli.aries.major.core.trigger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;
import pro.shushi.pamirs.trigger.annotation.Trigger;
import pro.shushi.pamirs.trigger.enmu.TriggerConditionEnum;

/**
 * 用户信息更新触发任务
 * 当用户数据被更新时，同步更新关联数据中的用户姓名
 */
@Slf4j
@Component
@Model.model(DemoUser.MODEL_MODEL)
public class DemoUserTrigger {

    @Autowired
    private DemoUserSyncService demoUserSyncService;

    @Trigger(displayName = "用户更新后同步关联数据",
             name = DemoUser.MODEL_MODEL + "#Trigger#onUpdate",
             condition = TriggerConditionEnum.ON_UPDATE)
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoUser onUpdate(DemoUser before, DemoUser after) {
        log.info("用户更新触发任务执行，用户ID: {}", after.getId());

        // 对比 before 和 after，判断关键字段是否变更
        if (after.getName() != null && !after.getName().equals(before.getName())) {
            log.info("用户姓名发生变更: {} -> {}", before.getName(), after.getName());
            // 同步更新关联数据中的用户姓名
            demoUserSyncService.syncUserName(after);
        }

        return after;
    }
}
```

### 监听状态变更的 ON_UPDATE

```java
@Slf4j
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderStatusTrigger {

    @Trigger(displayName = "订单状态变更后触发",
             name = DemoOrder.MODEL_MODEL + "#Trigger#onStatusUpdate",
             condition = TriggerConditionEnum.ON_UPDATE)
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoOrder onStatusUpdate(DemoOrder before, DemoOrder after) {
        // 仅在状态字段变更时执行逻辑
        if (before.getStatus() != null && !before.getStatus().equals(after.getStatus())) {
            log.info("订单状态变更: {} -> {}，订单ID: {}",
                     before.getStatus(), after.getStatus(), after.getId());
            // TODO: 根据新状态执行不同的业务逻辑
        }
        return after;
    }
}
```

## ON_DELETE 触发 — 数据删除时

### 基本示例

```java
package pro.shushi.deli.aries.item.core.trigger;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;
import pro.shushi.pamirs.trigger.annotation.Trigger;
import pro.shushi.pamirs.trigger.enmu.TriggerConditionEnum;

/**
 * 商品删除触发任务
 * 当商品数据被删除时，清理关联的缓存和搜索索引
 */
@Slf4j
@Component
@Model.model(DemoItem.MODEL_MODEL)
public class DemoItemTrigger {

    @Trigger(displayName = "商品删除后清理关联数据",
             name = DemoItem.MODEL_MODEL + "#Trigger#onDelete",
             condition = TriggerConditionEnum.ON_DELETE)
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoItem onDelete(DemoItem data) {
        log.info("商品删除触发任务执行，商品ID: {}", data.getId());
        // TODO: 清理关联缓存、搜索索引、外部系统数据等
        return data;
    }
}
```

## 同一类中定义多个触发方法

```java
@Slf4j
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderTrigger {

    @Trigger(displayName = "订单创建后触发",
             name = DemoOrder.MODEL_MODEL + "#Trigger#onCreate",
             condition = TriggerConditionEnum.ON_CREATE)
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoOrder onCreate(DemoOrder data) {
        log.info("订单创建触发，ID: {}", data.getId());
        // 创建后逻辑
        return data;
    }

    @Trigger(displayName = "订单更新后触发",
             name = DemoOrder.MODEL_MODEL + "#Trigger#onUpdate",
             condition = TriggerConditionEnum.ON_UPDATE)
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoOrder onUpdate(DemoOrder before, DemoOrder after) {
        log.info("订单更新触发，ID: {}", after.getId());
        // 更新后逻辑
        return after;
    }

    @Trigger(displayName = "订单删除后触发",
             name = DemoOrder.MODEL_MODEL + "#Trigger#onDelete",
             condition = TriggerConditionEnum.ON_DELETE)
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoOrder onDelete(DemoOrder data) {
        log.info("订单删除触发，ID: {}", data.getId());
        // 删除后逻辑
        return data;
    }
}
```

## 接口 + 实现类模式（模式 B）

适用于需要接口定义和远程调用的场景：

### 接口定义（放在 -api 模块）

```java
package pro.shushi.deli.aries.major.api.function;

import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.trigger.annotation.Trigger;
import pro.shushi.pamirs.trigger.enmu.TriggerConditionEnum;

@Fun(DemoUser.MODEL_MODEL)
public interface DemoUserUpdateTriggerFunction {

    @Function
    @Trigger(name = "DemoUserUpdateTriggerFunctionImpl-onUpdate",
             displayName = "用户更新触发-同步关联数据中的用户信息",
             condition = TriggerConditionEnum.ON_UPDATE)
    DemoUser onUpdate(DemoUser before, DemoUser after);
}
```

### 实现类（放在 -core 模块）

```java
package pro.shushi.deli.aries.major.core.trigger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.trigger.annotation.Trigger;
import pro.shushi.pamirs.trigger.enmu.TriggerConditionEnum;

@Slf4j
@Service
@Fun(DemoUser.MODEL_MODEL)
public class DemoUserUpdateTriggerFunctionImpl implements DemoUserUpdateTriggerFunction {

    @Autowired
    private DemoUserSyncService demoUserSyncService;

    @Override
    @Function
    @Trigger(name = "DemoUserUpdateTriggerFunctionImpl-onUpdate",
             displayName = "用户更新触发-同步关联数据中的用户信息",
             condition = TriggerConditionEnum.ON_UPDATE)
    public DemoUser onUpdate(DemoUser before, DemoUser after) {
        if (after != null && after.getId() != null) {
            log.info("用户更新触发任务执行，用户ID: {}", after.getId());
            demoUserSyncService.syncUserInfo(after);
        }
        return after;
    }
}
```

## 骨架模板（用户仅提供目的描述时使用）

当用户只描述了目的但未提供具体技术信息时，生成以下骨架代码，用 TODO 标注需要用户填充的部分：

```java
package pro.shushi.deli.aries.{module}.core.trigger;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;
import pro.shushi.pamirs.trigger.annotation.Trigger;
import pro.shushi.pamirs.trigger.enmu.TriggerConditionEnum;

/**
 * TODO: 补充触发任务的业务说明
 *
 * 前置条件：
 * - 确认 canal 中间件已监听 {TargetModel} 对应的数据库表
 * - 确认 RocketMQ 已部署且连接正常
 * - 确认 yml 中 pamirs.event.enabled: true
 * - 确认 trigger 模块已加入 pamirs.boot.modules
 */
@Slf4j
@Component
@Model.model(TargetModel.MODEL_MODEL) // TODO: 替换为实际的目标模型
public class TargetModelTrigger {

    // TODO: 注入需要的服务
    // @Autowired
    // private XxxService xxxService;

    @Trigger(displayName = "TODO: 填写触发任务显示名称",
             name = TargetModel.MODEL_MODEL + "#Trigger#onCreate", // TODO: 确认触发条件对应的事件名
             condition = TriggerConditionEnum.ON_CREATE) // TODO: 选择触发条件 ON_CREATE / ON_UPDATE / ON_DELETE
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public TargetModel onCreate(TargetModel data) { // TODO: ON_UPDATE 时改为 (TargetModel before, TargetModel after)
        log.info("触发任务执行，数据ID: {}", data.getId());

        try {
            // TODO: 在此编写触发后的业务逻辑
            //
            // 常见场景：
            // 1. 发送通知（邮件、短信、站内信）
            // 2. 同步数据到外部系统（ERP、CRM）
            // 3. 更新关联数据
            // 4. 写入审计日志
            // 5. 触发下游流程

        } catch (Exception e) {
            log.error("触发任务执行异常，数据ID: {}", data.getId(), e);
            // 注意：不要抛出未捕获异常，否则会触发重试
        }

        return data;
    }
}
```

## 注意：避免循环触发

如果触发方法中修改了同模型的数据（比如 ON_UPDATE 中调用 updateById），会导致再次触发 ON_UPDATE，形成无限循环。避免方式：

```java
@Trigger(displayName = "订单更新后触发",
         name = DemoOrder.MODEL_MODEL + "#Trigger#onUpdate",
         condition = TriggerConditionEnum.ON_UPDATE)
@Function(openLevel = FunctionOpenEnum.LOCAL)
@Function.Advanced(type = FunctionTypeEnum.UPDATE)
public DemoOrder onUpdate(DemoOrder before, DemoOrder after) {
    // 方式 1：通过字段对比判断是否需要执行，避免无意义的重复触发
    if (before.getStatus() != null && before.getStatus().equals(after.getStatus())) {
        // 状态未变更，跳过
        return after;
    }

    // 方式 2：使用标记字段，避免修改同模型数据时再次触发
    // 将需要更新的逻辑移到其他模型或使用直接 SQL 绕过 ORM

    return after;
}
```
