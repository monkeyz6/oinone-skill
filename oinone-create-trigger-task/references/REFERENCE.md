# Oinone 触发任务 完整注解参数与约束规则

## Import 清单

触发任务类需要的完整 import（按实际使用选取）：

```java
// Spring 组件注册（二选一）
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

// Pamirs 注解
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;

// Trigger 注解（注意：包名是 enmu，不是 enum，这是框架的命名）
import pro.shushi.pamirs.trigger.annotation.Trigger;
import pro.shushi.pamirs.trigger.enmu.TriggerConditionEnum;

// 可选：依赖注入
import org.springframework.beans.factory.annotation.Autowired;
```

**包名特别说明**：`pro.shushi.pamirs.trigger.enmu` 和 `pro.shushi.pamirs.meta.enmu` 中的 `enmu` 不是拼写错误，是框架实际的包命名，必须保持原样。

## @Trigger 注解详解

### 注解定义

来源：`pro.shushi.pamirs.trigger.annotation.Trigger`

作用于方法级别（`@Target({ElementType.METHOD})`），运行时保留。

### 全部参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `name` | String | 是 | — | 技术名称，全局唯一标识。安装后不可变更 |
| `displayName` | String | 是 | — | 显示名称，用于管理界面和日志 |
| `condition` | TriggerConditionEnum | 是 | — | 触发条件（创建/更新/删除） |
| `active` | boolean | 否 | true | 是否启用。设为 false 可临时禁用触发 |
| `wiredContext` | String | 否 | "" | 自动装配上下文参数名。高级用法，一般不需要 |
| `eventParameter` | String | 否 | "" | 事件ID参数名。用于事件追踪，方法需有对应的 String 参数 |
| `taskType` | TaskType | 否 | BASE_SCHEDULE_TASK | 任务类型。一般不需要修改 |

### name 命名规范

name 是触发任务的全局唯一标识，安装到系统后不可变更。推荐两种命名格式：

**格式 A（推荐，与模型绑定）：**
```java
name = DemoOrder.MODEL_MODEL + "#Trigger#onCreate"
// 运行时值类似："demo.DemoOrder#Trigger#onCreate"
```

**格式 B（与实现类绑定，框架内部常用）：**
```java
name = "DemoOrderTrigger-onCreate"
```

选择哪种格式不影响功能，关键是全局唯一。同一个类中的多个触发方法，name 必须各不相同。

## TriggerConditionEnum 枚举详解

来源：`pro.shushi.pamirs.trigger.enmu.TriggerConditionEnum`

| 枚举值 | 值 | 显示名 | 默认方法名 | 说明 |
|--------|-----|--------|-----------|------|
| `ON_CREATE` | "ON_CREATE" | 创建时 | onCreate | 模型数据被创建时触发 |
| `ON_UPDATE` | "ON_UPDATE" | 更新时 | onUpdate | 模型数据被更新时触发 |
| `ON_DELETE` | "ON_DELETE" | 删除时 | onDelete | 模型数据被删除时触发 |

## 方法签名规范

### ON_CREATE（创建触发）

```java
public TargetModel onCreate(TargetModel data)
```
- `data`：新创建的模型实例，包含所有字段值
- 返回值：必须返回模型实例（通常直接 return data）

**带事件追踪的变体：**
```java
// 需配合 @Trigger(eventParameter = "msgId")
public TargetModel onCreate(TargetModel data, String msgId)
```

### ON_UPDATE（更新触发）

```java
public TargetModel onUpdate(TargetModel before, TargetModel after)
```
- `before`：更新前的模型实例（旧值）
- `after`：更新后的模型实例（新值）
- 返回值：必须返回模型实例（通常 return after）
- 可通过对比 before 和 after 判断哪些字段发生了变更

### ON_DELETE（删除触发）

```java
public TargetModel onDelete(TargetModel data)
```
- `data`：被删除的模型实例
- 返回值：必须返回模型实例（通常直接 return data）

## 类级别注解详解

### 模式 A：@Component + @Model.model（推荐）

适用于业务项目中的触发任务。简洁，不需要额外的接口定义。

```java
@Slf4j
@Component
@Model.model(TargetModel.MODEL_MODEL)
public class TargetModelTrigger {
    // 触发方法
}
```

- `@Component`：Spring 组件注册
- `@Model.model(MODEL_MODEL)`：绑定到目标模型，框架据此关联触发事件
- `@Slf4j`：Pamirs 提供的日志注解（`pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j`，不是 Lombok 的）

### 模式 B：@Service + @Fun + 接口（框架级别）

适用于需要接口定义和远程调用的场景。框架内部工作流模块使用此模式。

```java
// 1. 定义接口
@Fun(TargetModel.MODEL_MODEL)
public interface TargetModelTriggerFunction {
    @Function
    @Trigger(name = "...", displayName = "...", condition = TriggerConditionEnum.ON_UPDATE)
    TargetModel onUpdate(TargetModel before, TargetModel after);
}

// 2. 实现类
@Service
@Fun(TargetModel.MODEL_MODEL)
public class TargetModelTriggerFunctionImpl implements TargetModelTriggerFunction {
    @Override
    @Function
    @Trigger(name = "...", displayName = "...", condition = TriggerConditionEnum.ON_UPDATE)
    public TargetModel onUpdate(TargetModel before, TargetModel after) {
        // 业务逻辑
        return after;
    }
}
```

- `@Fun(MODEL_MODEL)`：通过函数命名空间绑定到模型（接口和实现类都需要）
- `@Service`：Spring 服务注册
- 接口和实现类上的 `@Trigger` 注解参数需保持一致

### 两种模式的选择依据

| 场景 | 推荐模式 |
|------|---------|
| 普通业务触发任务 | 模式 A（@Component + @Model.model） |
| 需要 Dubbo 远程调用的触发任务 | 模式 B（@Service + @Fun + 接口） |
| 触发逻辑可能被其他模块复用 | 模式 B |
| 快速原型开发 | 模式 A |

## 方法级别配合注解

触发方法上必须同时标注以下注解：

### @Function

```java
@Function(openLevel = FunctionOpenEnum.LOCAL)
```

- 将方法注册到 Pamirs 函数注册表
- `openLevel = FunctionOpenEnum.LOCAL`：触发任务通常只需本地调用
- 如果使用模式 B 且需要远程调用，可设为 `{FunctionOpenEnum.LOCAL, FunctionOpenEnum.REMOTE}`

### @Function.Advanced

```java
@Function.Advanced(type = FunctionTypeEnum.UPDATE)
```

- 标记函数类型为更新操作
- 触发任务通常涉及数据处理，使用 `UPDATE` 类型

## 触发任务执行特性

理解以下特性有助于正确使用触发任务：

1. **异步执行**：触发任务通过 RocketMQ 投递消息，由 TBSchedule 调度消费，不在原操作的事务中执行
2. **最终一致性**：触发执行有延迟（通常毫秒到秒级），不保证实时性
3. **幂等性要求**：消息可能重复投递，触发逻辑应具备幂等性
4. **无事务关联**：触发任务的执行成功与否不影响原操作的事务提交
5. **数据库层面监听**：canal 监听的是数据库 binlog，任何对数据库的直接修改（包括非 ORM 操作）都会触发

## 常见约定和注意事项

1. **返回值不能为 void**：触发方法必须有返回值，通常返回模型实例
2. **一个类可定义多个触发方法**：同一个 Trigger 类可以包含 onCreate、onUpdate、onDelete 等多个方法
3. **name 全局唯一**：同一应用中所有 @Trigger 的 name 值不能重复
4. **JDK 8 兼容**：代码必须使用 JDK 8 语法
5. **不要在触发方法中抛出未捕获异常**：异常会导致触发任务失败重试，应该 try-catch 并记录日志
6. **避免循环触发**：如果触发方法中修改了同模型的数据，可能导致再次触发，需注意避免无限循环
