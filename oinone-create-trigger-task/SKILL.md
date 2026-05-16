---
name: oinone-create-trigger-task
description: Use when creating oinone-pamirs trigger tasks that react to data changes (create/update/delete) via canal binlog monitoring. Triggered by requests involving "创建触发任务", "触发任务", "数据变更触发", "监听数据变化", "binlog触发", "当数据创建/更新/删除时", "数据变更回调", or any scenario where a model's data change should automatically trigger an asynchronous operation. This skill is specifically for @Trigger annotation-based tasks, NOT for scheduled tasks (@XSchedule), async tasks (@XAsync), or ScheduleAction patterns.
compatibility: Designed for oinone-pamirs projects (deli-aries / kailas-aries) running with canal + RocketMQ + TBSchedule infrastructure. Requires JDK 8 and the pamirs-trigger module on the classpath.
metadata:
  framework: oinone-pamirs
  pattern: "@Trigger"
  version: "3.0"
---

# Oinone 触发任务（@Trigger）创建

生成一个绑定到模型的 `@Trigger` 类，由 canal binlog 事件异步触发，链路：MySQL → canal → RocketMQ → TBSchedule。

## 验收标准

代码生成完成时，agent 必须逐条自检通过：

- [ ] 注解三件套齐全：`@Trigger(...)` + `@Function(openLevel = FunctionOpenEnum.LOCAL)` + `@Function.Advanced(type = FunctionTypeEnum.UPDATE)`
- [ ] 触发条件与方法签名匹配：`ON_CREATE` / `ON_DELETE` → 单参 `(Model data)`；`ON_UPDATE` → 双参 `(Model before, Model after)`
- [ ] 方法返回模型实例，**不为 void**（框架不接受 void 返回）
- [ ] 类级绑定：模式 A `@Component + @Model.model(MODEL_MODEL)` 或模式 B `@Service + @Fun + 接口 impl`，**不可** `@Component + @Fun` 无 implements 的混用
- [ ] `@Slf4j` 来源是 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j`（非 Lombok，非 `org.slf4j.LoggerFactory`）
- [ ] catch 块内**不 rethrow**（rethrow 会被 MQ 视作失败并触发雪崩重试）
- [ ] `@Trigger.name` 全局唯一，推荐格式：`MODEL_MODEL + "#Trigger#" + methodName`
- [ ] 类放在业务域 `-core` 模块的 `trigger/` 子包下

## 不做什么

| 误用场景 | 应使用 |
|---|---|
| 周期/定时跑批 | `oinone-create-schedule-task`（ScheduleAction） |
| 普通后台异步、解耦下游 | `oinone-create-async-task`（@XAsync） |
| 同事务同步切面（操作前后扩展） | `oinone-create-hook`（HookBefore/HookAfter） |
| Pamirs 5.x 写 `@XSchedule(cron=...)` | 框架不支持 cron，必须用 ScheduleAction |
| 在 ON_UPDATE 中 `updateById` 同模型 | 会再次触发 ON_UPDATE 死循环；改其他模型 / 直接 SQL 绕 ORM / 字段对比跳过 |

## 工作模式

**触发任务链路重**（canal + MQ + 调度），生成前先评估替代方案；只有真的需要"监听 DB 层变更并异步响应"才用 @Trigger。

```
信息是否完备？
├─ 完备（模型 + MODEL_MODEL + 触发条件 + 业务逻辑都已知）
│  → 直接生成代码，注释里列出基础设施前置条件让用户确认
└─ 缺失关键信息（目标模型 / 触发条件 / 业务意图模糊）
   → 用 AskUserQuestion 单次问清最少必要信息后再生成
```

**模式 A vs 模式 B 决策**：
- 默认模式 A（`@Component + @Model.model`）— 业务代码首选，简洁
- 仅当需要 Dubbo 远程暴露 / 跨模块复用接口 → 模式 B（`@Service + @Fun + 接口`）

**基础设施前置条件**（在生成代码的 Javadoc 顶部注明，供用户运维确认）：
- canal 监听目标模型对应的库表
- RocketMQ 部署正常
- yml: `pamirs.event.enabled: true`
- `trigger` 加入 `pamirs.boot.modules`

## 已知陷阱

| 陷阱 | 具体表现 | 应对 |
|---|---|---|
| catch 后 rethrow 触发 MQ 重试雪崩 | `catch (Exception e) { log; throw e; }` | catch + log，**不**重抛；框架不靠异常驱动重试 |
| ON_UPDATE 用单参签名 | `onUpdate(Model data)` | 必须 `onUpdate(Model before, Model after)`，框架按双参注入 |
| `eventParameter` 误用 | `eventParameter = DemoOrder.MODEL_MODEL` | `eventParameter` 是**方法上 String 参数名**用于事件追踪（如 `msgId`），**不是模型标识符**。模型绑定走 `@Model.model` |
| `@Slf4j` 走 Lombok 或 org.slf4j | `import lombok.extern.slf4j.Slf4j` 或 `LoggerFactory.getLogger(...)` | 必须 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j` |
| 包名拼成 `enum` | `import pro.shushi.pamirs.trigger.enum.*` | 框架包名就是 `enmu`（不是拼写错误），保持原样 |
| 类绑定混用 | `@Component + @Fun(MODEL_MODEL)` 但没有 implements | 模式 A 用 `@Model.model`；模式 B 必须 `@Service + @Fun + interface impl` |
| 方法返回 void | `public void onCreate(...)` | 必须返回模型实例（`return data` / `return after`） |
| `@Trigger.name` 不唯一 | `name = "onCreate"` 短名易冲突 | 用 `MODEL_MODEL + "#Trigger#" + methodName`；安装后不可改 |
| 自造内存缓存判变更 | ON_UPDATE 中 `ConcurrentHashMap<id, lastValue>` 比对 | 直接 `Objects.equals(before.getName(), after.getName())`，框架已提供前后镜像 |
| 触发方法被多副本重复消费 | 单条 binlog 在多实例消费 | 触发逻辑写成幂等（按 id 覆盖写、`evict`、`upsert` 等） |

## 触发条件 × 方法签名速查

| 条件 | 方法签名 |
|---|---|
| `ON_CREATE` | `Model onCreate(Model data)` |
| `ON_UPDATE` | `Model onUpdate(Model before, Model after)` |
| `ON_DELETE` | `Model onDelete(Model data)` |

带事件追踪的变体：方法加 `String msgId` 参数 + `@Trigger(eventParameter = "msgId")`。

## 参考文档（按需加载）

- [references/REFERENCE.md](references/REFERENCE.md) — `@Trigger` 全参数、`TriggerConditionEnum` 枚举值、import 清单、模式 A/B 详细对比、执行特性（异步/最终一致/幂等）
- [references/EXAMPLES.md](references/EXAMPLES.md) — ON_CREATE/ON_UPDATE/ON_DELETE 完整代码模板、接口+实现类模式、骨架模板、避免循环触发的两种写法
