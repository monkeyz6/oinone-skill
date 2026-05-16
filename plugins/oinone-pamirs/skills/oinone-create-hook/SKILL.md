---
name: oinone-create-hook
description: Use when creating oinone-pamirs function interceptors (HookBefore / HookAfter). Triggered by "创建拦截器", "前置拦截", "后置拦截", "HookBefore", "HookAfter", "@Hook", "拦截XX函数", "在XX函数执行前做XX", "在XX函数执行后做XX", "对XX函数加切面", "hook", "interceptor", or any scenario where a non-invasive before/after logic extension is needed on an existing function. Covers ONLY function hooks (HookBefore/HookAfter), NOT SPI extension points (ExtPoint), NOT trigger tasks (@Trigger), NOT async tasks (@XAsync). Even if the user doesn't explicitly say "Hook" or "拦截器", trigger this skill when they describe a need to execute logic before or after an existing Pamirs function without modifying the original code.
compatibility: Designed for oinone-pamirs aPaaS projects (Java 8 + Pamirs 5.3.x). Generated code depends on `pro.shushi.pamirs.meta.*` APIs and must live in a `-core` module that is consumed as a jar dependency by the `-boot` module.
metadata:
  author: deli-aries-team
  version: "2.0"
  framework: oinone-pamirs
---

# Oinone 函数拦截器（HookBefore / HookAfter）

在 oinone-pamirs 已有函数前后插入非侵入式逻辑。

## 验收标准

完成生成后逐条自检；任何一条未达成就还没完成。

- [ ] 类 `implements HookBefore` 或 `implements HookAfter`（**它们是接口，不是注解**）
- [ ] HookBefore/HookAfter import 来自 `pro.shushi.pamirs.meta.api.core.faas.*`
- [ ] `@Hook` 注解放在 `run` 方法上，**不是**类上
- [ ] `@Hook` 业务必填属性齐全：`priority` + `model={X.MODEL_MODEL}` + `fun="..."`（无过滤的全局 Hook 仅在用户明确要求时生成）
- [ ] `@Slf4j` 来自 `pro.shushi.pamirs.meta.annotation.fun.extern`，**不是** Lombok
- [ ] HookBefore 方法签名 `run(Function function, Object... args)`
- [ ] HookAfter 方法签名 `run(Function function, Object ret)`，且对 `ret` 做 `instanceof Object[]` 解包后再处理
- [ ] HookAfter 完成后 `return ret;`（**不是** `return data;` 或 `return null;` — 后者会把业务返回值置空）
- [ ] 类有 `@Component`；包路径在某个 `-core` 模块的 `hook` 子包下
- [ ] Javadoc 至少含 1 条 Pamirs 特定提示：jar 依赖 / `enableHook` / `module` 入口语义 三选一

## 不做什么

| 不覆盖 | 应改用 |
|---|---|
| 替换函数默认实现 | SPI 扩展点 `ExtPoint` |
| 数据库 binlog 数据变更回调 | `oinone-create-trigger-task` |
| 后台异步执行 | `oinone-create-async-task` |
| 服务端按钮 / 表单 / 批量动作 | `oinone-create-action` |

## 决策矩阵

按场景判断，不要逐项追问用户：

| 决策点 | 默认 | 何时改 |
|---|---|---|
| 拦截类型 | 动词判断："执行前 / 校验 / 阻断" → HookBefore；"执行后 / 记录 / 脱敏 / 增强" → HookAfter | — |
| 目标 model + fun | 取用户上下文里的 `MODEL_MODEL` 常量 + 函数编码 | 信息不全见下节 |
| `priority` | `100`（普通业务） | "最后执行 / 审计 / 日志" → `Integer.MAX_VALUE`；"先于其他业务" → `50` |
| `module` 过滤 | 不配（所有入口生效） | 仅在某前端入口（mall / merchant 等）生效时再配 |
| 包路径 | `{项目包}.{业务域}.core.hook`，例：`pro.shushi.deli.aries.trade.core.hook` | — |

## 信息缺失时的策略

- **上下文已含模型名 + 函数名 + 包路径** → 直接生成。
- **信息不全且能问** → 用 `AskUserQuestion` 工具问（不是纯文本提问）。
- **信息不全且无法问**（subagent 等环境）→ 用合理默认 + `// TODO ysy 待确认` 占位，并在响应中列出待确认字段。
- **用户描述与决策矩阵冲突**（例如显式要求"对所有函数生效"）→ 按用户意图生成，但 Javadoc 写明此偏离及风险。

## 代码硬约束

- 拦截器类必须在某个 `-core` 模块（以 jar 被 `-boot` 引入），否则运行时不生效。
- JDK 8 语法兼容，**禁止** Lombok（`@Data` / `lombok.Slf4j`）。
- 错误码用 `PamirsException.construct(SomeExpEnumerate.X).errThrow()`，不要 `appendMsg("...")` 拼字符串绕过枚举。

## 必须写入产出代码的安全提示

每个生成的拦截器类，Javadoc 至少含与场景相关的 1-2 条：

1. **过滤属性必填** — 未配 `model` / `fun` 会对全系统函数生效。
2. **后端调用默认不触发 Hook** — 编程式调用需 `PamirsSession.directive().enableHook()`。
3. **必须 jar 依赖** — 拦截器所在模块要被 `-boot` 模块以 jar 引入。
4. **`module` 是请求入口** — 不是拦截器所在模块；不配则匹配全部入口。

## 已知陷阱

| 陷阱 | 怎么识别 | 应对 |
|---|---|---|
| 把 `HookBefore` / `HookAfter` 当注解用 | 写成 `@HookAfter(model=...)` 修饰方法，类没 `implements` | 它们是**接口**，类必须 `implements`；注解只用 `@Hook` |
| HookAfter 直接强转 `ret` | 没有 `instanceof Object[]` 判断 | 必有 Object[] 解包：`if (ret instanceof Object[]) { Object[] rets = (Object[]) ret; data = rets[0]; }` |
| Lombok `@Slf4j` | import `lombok.extern.slf4j.Slf4j` | 改成 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j` |
| `@Hook` 参数名错 | 写成 `name="create"` | 正确是 `fun="create"` |
| HookBefore 返回 `null` 想阻断 | 只有 `return null;` 没有 `throw` | 阻断必须 `throw new PamirsException(...)`；`return null` 不阻断 |
| HookAfter 返回 `data` 或 `null` | `return data;` / `return null;` | 必须 `return ret;`；解包后 `data` 是 `ret` 内引用，原地修改即可；返 `data` 会让框架把业务返回值置空 |
| 后端调用静默不触发 | 业务方法被直接 `service.method()` 调用，Hook 没跑 | 调用前 `PamirsSession.directive().enableHook()` |
| 副作用 Hook 抛异常带崩主流程 | 审计 / 日志 / 通知逻辑没有 try/catch | 整段包 try/catch + `log.error(...)`，不再向上抛 |

## 参考资料（按需加载）

- [references/REFERENCE.md](references/REFERENCE.md) — `@Hook` 全部参数语义、import 完整清单、`priority` 取值惯例、执行特性、与 ExtPoint / Trigger 对比
- [references/EXAMPLES.md](references/EXAMPLES.md) — HookBefore / HookAfter 完整代码模板（含 module 过滤、修改入参、Object[] 解包、Pagination 处理、审计日志、骨架）
