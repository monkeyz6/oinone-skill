---
name: oinone-create-schedule-task
description: >
  Generate oinone-pamirs scheduled / periodic tasks via the ScheduleAction pattern (Pamirs 5.x, NO cron).
  Use for any background job on a fixed cadence inside deli-aries / kailas-aries — e.g. "每 15 分钟同步库存",
  "每天凌晨 3 点跑一次", "定时签收", "延迟任务". Produces a `ScheduleAction` class (`@Component` + `@Fun`,
  with `initTask()` + `execute()`) plus an `InstallDataInit` + `UpgradeDataInit` lifecycle class that
  registers the schedule at app start. Trigger keywords: 创建定时任务, 定时执行, 周期任务, 定期执行,
  定时调度, 定时同步, 延迟任务, 定时跑批, scheduled task, periodic task, recurring job, background job.
  Match on intent even without "定时" (固定频率执行某动作). Covers ONLY ScheduleAction tasks.
  NOT for `@Trigger` binlog-driven jobs (use oinone-create-trigger-task), NOT for `@XAsync` / ExecuteTaskAction
  async work (use oinone-create-async-task), NOT for the legacy `@XSchedule(cron=...)` annotation.
compatibility: Designed for oinone-pamirs 5.x (Java 8 + Spring Boot 2.3) projects deli-b2b-oversea and kailas-aries-oversea. No cron expressions.
license: Proprietary — internal use only.
metadata:
  author: deli-aries-team
  version: "2.2"
  domain: oinone-pamirs-schedule
---

# Oinone 定时任务代码生成

产出两个 Java 文件：`ScheduleAction` 实现类（Task）+ 应用生命周期注册类（Init）。

## 验收标准（生成后逐条自查）

- [ ] Task 类同时挂 `@Component`、`@Fun(FUN_NAMESPACE)`、`@Slf4j` 三个注解
- [ ] `@Slf4j` 来源是 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j`（**不是** `lombok.extern.slf4j.Slf4j`）
- [ ] 实现 `ScheduleAction`，`execute(ScheduleItem)` 返回 `Result<Void>`，并 `setSuccess(true)` / `setFail(msg)` 双路径都有
- [ ] `initTask()` 末尾**必须**调用 `scheduleTaskActionService.submit(action)`，否则任务永不落库
- [ ] 周期三件套齐：`setPeriodTimeValue` + `setPeriodTimeUnit(TimeUnitEnum.*)` + `setPeriodTimeAnchor(TriggerTimeAnchorEnum.*)`
- [ ] `setTaskType(TaskType.XXX.getValue())` —— 通过 `getValue()` 取字符串值，不是直接传枚举
- [ ] Init 类同时实现 `InstallDataInit` 与 `UpgradeDataInit`；`upgrade()` 委托 `init()`
- [ ] `modules()` 返回 `List<String>`，元素为项目里的 `XxxModule.MODULE_MODULE` 常量
- [ ] 整文件 grep 不到 `setCron(`、`@XSchedule`、cron 字面量（如 `0 0/15 * * * ?`）
- [ ] 与 Task 代码同目录输出 `NOTES.md`：上述每条勾选状态 + FUN_NAMESPACE / 周期参数 / taskType / 超时 / 首次执行 / `modules()` / 默认选项偏离说明 — 让用户和后续 review 能一眼看完整面貌

## 不做什么（边界）

| 场景 | 该用 |
|---|---|
| 数据变更才跑（订单状态变了通知销售）| `oinone-create-trigger-task`（canal binlog 监听）|
| 把当前请求里的某段逻辑丢后台跑一次 | `oinone-create-async-task`（`@XAsync` 或 ExecuteTaskAction）|
| cron 表达式 | 不支持，必须用 `periodTimeValue` + `periodTimeUnit` + `periodTimeAnchor` 三件套 |

## 工作模式

### 必须向用户确认（用 `AskUserQuestion`，禁止自己拍）

| # | 决策点 | 必问理由 |
|---|---|---|
| 1 | 归属模块 | 决定 Task 包路径、Init 的 `modules()` 返回值、FUN_NAMESPACE 命名空间。提供选项时列出项目里已有的 `XxxModule.MODULE_MODULE` 常量 |
| 2 | 中文显示名 + Java 类名 | 推导 `displayName`、类名、FUN_NAMESPACE |
| 3 | 执行频率 | 推导 `periodTimeValue` + `periodTimeUnit`。常见档：每 N 分钟 / 每 N 小时 / 每天 X 点 / 每周 / 单次延迟 |
| 4 | 首次执行时间 | 选项：立即 / 明天凌晨 X 点 / 今天 X 点 / 自定义时间 |
| 5 | Init 类合并 vs 独立 | 先 grep 目标模块下的 `*ScheduleInit*`；有则提议追加到 `initTasks()`，无则新建 |

### 可以默认（在 NOTES 里告知用户即可）

| 参数 | 默认 | 何时偏离 |
|---|---|---|
| `taskType` | `CYCLE_SCHEDULE_NO_TRANSACTION_TASK` | 单次/延迟任务改 `BASE_SCHEDULE_NO_TRANSACTION_TASK` |
| `periodTimeAnchor` | `START` | 仅当"等上次跑完再排下次"时用 `FINISHED` |
| `limitExecuteNumber` | `-1`（无限） | 单次任务改 `1` |
| `limitRetryNumber` / 间隔 | `1` 次 / `1 MINUTE` | 关键任务调高（典型：6 次 / 5 MINUTE）|
| `timeout` | 见下表 | — |
| `context` | `null` | 需要给 `execute()` 传业务数据时 `JsonUtils.toJSONString(POJO)` |

### `timeout` 推荐档（按任务画像选）

| 任务画像 | 建议（ms）|
|---|---|
| 纯 DB 查询 / 内存计算 | `5000` |
| 跨域 RPC / 中等批处理 | `50000` |
| 调外部 HTTP / 大批量同步 | `600000`（10 min）|
| 整库扫表 / 长跑统计 | `-1` 无超时 |

### 命名与包路径

- `FUN_NAMESPACE = "<project>.<module>.schedule.<ClassName>"`，例：`"deli.aries.eip.schedule.DeliAriesSyncZoroInventoryTask"`
- `METHOD_NAME = "execute"`（固定）
- `technicalName = FUN_NAMESPACE + "#" + METHOD_NAME`（最常用）；动态单次任务用 `FUN_NAMESPACE + "#" + bizId + "#" + 时间戳` 保唯一
- 包路径：`pro.shushi.<deli|kailas>.aries.<module>.core.task[.schedule|.execute]`

### 动态单次任务（如发货 2h 后查 TPL）

- 单 Task 类提供静态 `createTask(...)` 入口，业务代码调用以排任务
- 用 `BASE_SCHEDULE_NO_TRANSACTION_TASK` + `limitExecuteNumber=1`
- `context` 用 `JsonUtils.toJSONString(POJO)` 序列化业务数据；`execute()` 用 `JsonUtils.parseObject` 反序列化
- **通常不需要 Init 类**（不是启动时就跑，而是业务事件触发）；NOTES 里说明原因

## 已知陷阱（不踩坑表）

| 陷阱 | 怎么识别 | 应对 |
|---|---|---|
| `@Slf4j` 用了 Lombok | import 是 `lombok.extern.slf4j.Slf4j` | 必须 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j` |
| "顺手修正"框架包名 typo | 把 `enmu` 改成 `enum`、`eunmeration` 改成 `enumeration` | 框架原始包名就是拼错的，**保留原样**，否则 import 失败编译挂 |
| 忘记 `submit()` | `initTask()` 配完没调 `scheduleTaskActionService.submit(action)` | 任务永远不会落库，启动后查不到记录 |
| cron 偷渡 | `setCron(...)` / `@XSchedule(cron=...)` / cron 字面量 | Pamirs 5.x 不支持，全部走三件套 |
| `modules()` 类型错 | 返回 `List<XxxModule>` 或单 `String` | 类型必须 `List<String>`，元素是 `XxxModule.MODULE_MODULE` 常量值 |
| 单次任务用了周期 taskType | `limitExecuteNumber=1` 但 taskType 还是 `CYCLE_*` | 改 `BASE_SCHEDULE_NO_TRANSACTION_TASK` |
| `Calendar` 时区漂移 | 没用 `TimeZone.getTimeZone("GMT+8")` | 服务器 UTC 时凌晨 3 点会跑成下午，必须显式 GMT+8 |
| 同一 Task 被多个 Init 注册导致重复 | 不同模块 Init 都注入同一 Task | 提交前用 `scheduleTaskActionService.countByEntity(action)` 判幂等 |
| `execute()` 没兜异常 | 直接 throw 上去 | 必须 `try { ... setSuccess(true) } catch (Exception e) { setFail(e.getMessage()) }`，否则重试链路断 |

## 参考文档（按需加载）

- 完整字段表 / 枚举值 / import 列表 / firstExecuteTime 计算辅助方法 → [references/REFERENCE.md](references/REFERENCE.md)
- 6 种代表场景的完整代码 + 骨架模板（高频同步 / 每日定时 / 多任务合并 Init / 动态单次任务等）→ [references/EXAMPLES.md](references/EXAMPLES.md)
