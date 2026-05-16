---
name: oinone-create-async-task
description: Use this skill when the user needs to make any method or business flow execute asynchronously in oinone-pamirs (deli-aries / kailas-aries). Covers TWO modes — (1) simple fire-and-forget via the @XAsync annotation; (2) ordered/serialized async via ExecuteTaskAction + ScheduleAction, used when the task needs bizId-grouped serial execution, a custom task type for monitoring, or a serialized context payload. Trigger on phrases like 创建异步任务, 异步执行, 异步处理, 后台异步, 把XX改成异步, 顺序异步, 串行异步, @XAsync, ExecuteTaskAction, async task, background async, fire-and-forget — even when the user doesn't explicitly say "async" but describes detaching work from the main request (e.g. 推送ES, 通知下游, 写日志, 发邮件 without blocking the caller). NOT for periodic/scheduled jobs (use oinone-create-schedule-task), NOT for binlog-triggered jobs on data change (use oinone-create-trigger-task).
license: Proprietary. Internal use within the deli-b2b-oversea / kailas-aries-oversea projects.
compatibility: Designed for oinone-pamirs 5.3.6.2+ on Java 8 + Spring Boot 2.3.8. Requires the host project to include the pamirs-schedule module and a working ScheduleClient.
metadata:
  author: deli-aries / kailas-aries team
  version: "2.1"
  scope: oinone-pamirs async task generation
---

# oinone-create-async-task — Oinone 异步任务创建

oinone-pamirs 里"异步"有两条路径，**语义保证不同，必须先选对**：

| Mode | 机制 | 适用场景 |
|------|------|---------|
| **A. @XAsync** | Service 方法上加注解，AOP 自动提交到调度中心 | fire-and-forget：异步通知、刷缓存、写日志、发邮件 |
| **B. ExecuteTaskAction + ScheduleAction** | 手动构造 task 并 `ExecuteTaskActionService.submit()`，实现 `ScheduleAction` 执行 | bizId/bizCode 分组串行、自定义 TaskType、需序列化上下文 |

**不做什么**（互不混用）：
- ❌ 定时/周期任务 → `oinone-create-schedule-task`
- ❌ 数据变更触发（@Trigger，监听 binlog）→ `oinone-create-trigger-task`
- ❌ 函数前/后置拦截 → `oinone-create-hook`

---

## 验收标准（每次生成后逐条勾）

- [ ] `@Function` / `@Override` 在接口与实现都齐；Mode A 的 Impl 上有 `@XAsync`；Mode B 的实现类有 `@Component @Fun @Slf4j`
- [ ] `@Slf4j` import 是 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j`（**不是 Lombok**）
- [ ] `displayName` 是中文且非空
- [ ] `FUN_NAMESPACE` 字符串在接口 / 实现上**完全一致**
- [ ] Mode B：`bizCode`/`bizId` 的 setter 与用户原话字段名匹配
- [ ] Mode B：`ScheduleAction.execute(...)` 上**没有** `@Transactional`
- [ ] Mode B：`context` 用 `JsonUtils` 序列化，不直接塞 entity / 含懒加载的对象
- [ ] 编译通过：`mvn -pl <module> -am install -DskipTests -s ~/.m2/settings-changsha.xml -Pshushi`

---

## 决策（不确定就用 `AskUserQuestion`，禁止纯文本提问、禁止自行假设）

### D1. 选模式

用户已明说「按顺序异步」/「@XAsync」就跳过。否则按特征匹配：

| 需求特征 | 模式 |
|---------|------|
| 只要异步执行，不关心顺序、参数可直接传 | **A**（默认推荐） |
| 同一业务实体的操作必须按产生顺序串行 | B |
| 需要传复杂可序列化上下文（DTO/List） | B |
| 需要自定义 TaskType 做监控/筛选 | B |
| 需要精确控制首次执行时间 | B |

### D2-A. Mode A 参数（已确认的别再问）

1. **目标 Service**：先 grep `*Service.java`，已存在就在原文件追加方法（**禁止重复创建**）。
2. **方法签名**：方法名 `async` 前缀（`asyncUpdatePrice`、`asyncNotifyEsUpdate`）；参数=业务上下文；返回 `void`。
3. **`displayName`**（**必填**）：中文，用于监控页 / 日志。
4. **重试**：

   | 场景 | limitRetryNumber | nextRetryTimeValue | Unit |
   |------|:-:|:-:|:-:|
   | 不重试 | 0 / 不设置 | — | — |
   | **标准（默认）** | **3** | **60** | **SECOND** |
   | 关键业务 | 5 | 60 | SECOND |
   | 高频 | 10 | 60 | SECOND |
   | 长间隔 | 3 | 5 | MINUTE |

5. **延迟**（可选）：`delayTime` + `delayTimeUnit`，缺省立即执行。

### D2-B. Mode B 参数

1. **位置**：Task 类落 `{module}-core/src/main/java/.../task/`。
2. **命名**：中文 `displayName`（"订单确认回调任务"）→ 类名（`DeliAriesOutTradeOrderAckTask`）→ `FUN_NAMESPACE`（`"deli.aries.trade.schedule.DeliAriesOutTradeOrderAckTask"`）。
3. **TaskType**：
   - 不需要自定义类型 → `TaskType.BASE_SCHEDULE_NO_TRANSACTION_TASK.getValue()` 或 `TaskType.REMOTE_SCHEDULE_TASK.getValue()`。
   - 需要分组监控/筛选 → 新建 `XxxTask extends BaseScheduleNoTransactionTask` 并定义 `TASK_TYPE` 常量。先 grep `extends BaseScheduleNoTransactionTask` 看有无可复用。
4. **Context**：

   | 数据 | 序列化 | 反序列化 |
   |------|--------|---------|
   | 单 ID | `id.toString()` | `Long.valueOf(scheduleItem.getContext())` |
   | 复杂对象 | `JsonUtils.toJSONString(o)` | `JsonUtils.parseObject(ctx, Class.class)` |
   | 集合 | `JsonUtils.toJSONString(list)` | `JsonUtils.parseObject(ctx, List.class)` |

5. **分组键 bizId vs bizCode**：调度框架按"分组键 + 串行 TaskType"做同键串行。**按用户原话字段名选 setter**：

   | 用户表达 | 调用 |
   |---------|------|
   | 数据库主键 / 自增 ID（如 `orderId`） | `setBizId(Long)` |
   | 业务编码 / 单号 / 字符串键（`outOrderCode`、`tradeNo`） | `setBizCode(String)` |

   错配的代价：运维按用户给的字段名去监控台搜不到任务记录。

6. **重试**：`limitRetryNumber=2~3`，`nextRetryTimeValue=30~90 SECOND`。
7. **超时**（可选 `FunctionDefinition.setTimeout(ms)`）：默认 `5000`，中等 `50000`，长耗时 `600000`。
8. **首次执行时间**：立即 `System.currentTimeMillis()`；延迟则 `+ delayMs`。

### D3. 命名 & 包路径

| 元素 | 规则 | 示例 |
|------|------|------|
| @XAsync Interface | `XxxService` + `@Fun(FUN_NAMESPACE)`，落 `-api` | `pro.shushi.deli.aries.item.api.api.DeliAriesItemService` |
| @XAsync Impl | `XxxServiceImpl`，落 `-core` | `pro.shushi.deli.aries.item.core.service.DeliAriesItemServiceImpl` |
| `FUN_NAMESPACE` | `"项目.模块.接口/类名"` | `"deli.aries.item.DeliAriesItemService"` |
| 异步方法名 | `async` 开头 | `asyncNotifyEsUpdate` |
| ExecuteTaskAction 实现 | 落 `-core/.../task/` | `pro.shushi.deli.aries.trade.core.task.DeliAriesOutTradeOrderAckTask` |

Impl 方法的注解顺序：`@Function` → `@Override` → `@XAsync(displayName=..., limitRetryNumber=..., nextRetryTimeValue=...)`。Interface 方法只 `@Function`。

---

## Gotchas（必读 — 每条都有具体踩坑场景）

| # | 陷阱 | 表现 | 应对 |
|---|------|------|------|
| 1 | `@Slf4j` 用错包 | 用 Lombok 的 `lombok.extern.slf4j.Slf4j` | 改用 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j`；用错会导致函数注册异常或日志失效 |
| 2 | `@Model` 类加 Lombok | 业务字段 getter/setter 失效 | `@Model` 类禁止 `@Data`/`@Getter`/`@Setter`；框架已动态接管。Service / Task 实现类不是 `@Model`，可用 Lombok，但 `@Slf4j` 仍必须 Pamirs 的 |
| 3 | `@Function` 漏掉 | 调用方拿到 `null` | 接口和实现都加；Pamirs 没注册就调不到 |
| 4 | `@XAsync` 加在 Interface | 注解被框架忽略，同步执行 | 只加在 Impl 上 |
| 5 | `displayName` 省略 | 监控页 / 日志无法识别任务 | 必填中文描述 |
| 6 | `FUN_NAMESPACE` Interface ≠ Impl | 函数找不到 | 用 `Xxx.FUN_NAMESPACE` 常量引用，不要写裸字符串 |
| 7 | `@Fun` 字符串不规范 | 注册失败 | 用 `项目.模块.类名` 的 dot 分隔形式；参考同模块已有类做镜像 |
| 8 | context 塞 entity | 含懒加载/代理，序列化炸 | 用 `JsonUtils` + 纯 POJO DTO |
| 9 | 包路径不在 `@Module` 扫描范围 | Spring 都扫不到 | 落在 `packagePrefix` 下 |
| 10 | `ScheduleAction.execute` 加 `@Transactional` | 与调度框架自管事务冲突 | 删掉；调度框架按 task 维度自管 |
| 11 | 方法名过长拼接 | `pushAllZoroPlatformRelatedSkuInventoryToZoroEveryHour` | 业务化短名 `pushZoroInventory`；参见 CLAUDE.md |
| 12 | 循环里调 `@XAsync` 方法 | N 条任务记录，调度压力大 | 整批传 `List<id>` 一次走完 |
| 13 | bizCode/bizId 错配 | 用户说"按 `xxxCode` 串行"但写了 `setBizId(longId)` | 严格按用户原话字段名选 setter。错了运维按字段搜不到 |

---

## 产物 & 落点

**Mode A**：在已有 `*Service.java` 追加 `@Function` 声明 + 在已有 `*ServiceImpl.java` 追加 `@Function @Override @XAsync` 方法。两个文件都不存在才新建。

**Mode B**：`ScheduleAction` 实现类（含 `submitTask()` 入口 + `execute(ScheduleItem)` 处理）；自定义 TaskType 时再加一个 `extends BaseScheduleNoTransactionTask` 的定义类；对外暴露入口才加 Interface。

关键参数加 1 行中文注释；显而易见的代码不要注释。

---

## Progressive disclosure — 何时加载子文件

`SKILL.md` 之外的资料**按需读，不要预读**：

- **查注解属性 / 枚举值 / import 全清单** → [references/REFERENCE.md](references/REFERENCE.md)
- **要看完整可运行示例** → [references/EXAMPLES.md](references/EXAMPLES.md)：@XAsync 3 例（最简 / 标准重试 / 带延迟）、ExecuteTaskAction 3 例（独立 / 三件套 / Manager 分离）、骨架模板、陷阱长版
- **碰到 Gotchas 没覆盖的报错** → 回看 REFERENCE.md 的「Context 序列化/反序列化」和「Import 列表」

## 参考链接

- 开发手册：https://guide.oinone.top
- 调度模块：https://doc.oinone.top/?s=ScheduleAction
- 项目仓库约定：根目录 `CLAUDE.md` § Oinone Skill 优先规则
