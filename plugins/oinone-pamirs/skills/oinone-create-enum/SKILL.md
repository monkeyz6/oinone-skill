---
name: oinone-create-enum
description: Create or modify Oinone/Pamirs enums and data dictionaries (`@Dict` + `IEnum<T>`). Use when defining status fields, type fields, option lists, bit flags / multi-select tags, inheritable cross-module enums, or business error codes on Oinone models — even when the user does not explicitly say "enum" or "dictionary" (e.g. 加个订单状态字段, 支持多选权限, 定义错误码, 渠道类型要能被其他模块扩展). Covers `IEnum<String>`, `IEnum<Integer>`, `BitEnum`, `BaseEnum` (inheritable), and `ExpBaseEnum` + `@Errors`. Specific to the Oinone/Pamirs framework — do not use for plain Java enums in non-Pamirs code or for proxy-only constants.
license: Proprietary
metadata:
  scope: oinone-pamirs
  version: "2.1"
---

# Oinone 枚举（数据字典）

Oinone 通过 `@Dict` 注解 + `IEnum<T>` 接口把 Java 枚举接入平台数据字典。每个枚举值由三要素构成：`value`（持久化到数据库的值）、`displayName`（前端显示）、`help`（描述）。

## 验收标准

任务完成后逐条勾选——任一项为 ❌ 就没完成。

- [ ] **类型匹配业务**：选用的枚举类型与「选择枚举类型」表中的业务特征一一对应（如多选场景没有用单选 `IEnum`）。
- [ ] **三要素齐全**：每个枚举项都显式写出 `(value, displayName, help)`；`value` 是大写英文或稳定整数，不是中文。
- [ ] **`DICTIONARY` 常量**：类中声明 `public static final String DICTIONARY = "{ns}.{ClassName}";` 且 `@Dict(dictionary = X.DICTIONARY, …)` 引用它；grep 现有常量确认不重名。
- [ ] **BitEnum 专项**（仅 BitEnum 类型时）：所有 value 是 `1L << n`，且使用该枚举的模型字段是 `List<E>` + `multi = true`。
- [ ] **`@Field.Enum` 无 `dictionary` 参数**：枚举字段上不要写 `dictionary = X`，平台从字段类型自动推断。
- [ ] **`defaultValue` 用存储值**：`@Field(defaultValue = "DRAFT")` 不是 `"草稿"`。
- [ ] **错误码专项**（仅 `ExpBaseEnum` 类型时）：用 `@Errors` 而非 `@Dict`；错误码全局唯一整数；service 抛出方为 `PamirsException(EnumItem)` 或 `PamirsException.construct(EnumItem).appendMsg(诊断).errThrow()`，**不存在** `appendMsg` 伪造错误码（即没有枚举项却用 msg 字符串当错误源）的写法。
- [ ] **BaseEnum 专项**（仅 BaseEnum 类型时）：是普通 `class extends BaseEnum<E, T>`（不是 Java enum 语法）；声明 `serialVersionUID`；枚举项用 `create(name, value, displayName, help)` 工厂方法定义。
- [ ] **编译通过**：mvn compile 不报错。

## 不做什么

- **不写**非 Pamirs 项目里的普通 Java 枚举（无 `@Dict`、无 `IEnum`）。这种用 Java 原生 enum 即可，没必要套这个 skill。
- **不写**仅供代码内部使用、不持久化、不在 UI 渲染的常量集合——用 Java enum 或 `public static final` 常量更轻。
- **不改**已上线枚举的 `value()` 返回值（重命名/改值会让历史数据失配）；只能新增项。
- **不改**模型字段的 getter/setter（框架基类的 `get_d() / set_d()` 已经实现）；枚举类本身的方法不受此限。
- **不替用户决策**：包路径、`DICTIONARY` 命名空间、枚举项的存储值，必须问；调用 `AskUserQuestion` 列推荐选项，允许自定义（见"不确定时"）。
- **不耦合错误码到 `@Dict`**：业务错误码走 `ExpBaseEnum + @Errors`，与数据字典是两套体系。

## 工作流（决策框架，非脚本）

按业务语义决定类型，按项目惯例决定归属，按模板填充内容。Agent 自行判断各步是否完成。

1. **决定类型** — 用「选择枚举类型」表把业务场景映射到具体实现。表中没有的混合场景，参考 BaseEnum+BitEnum 行的组合规则。
2. **决定归属** — 包路径与 `DICTIONARY` 前缀**不自己造**，按"不确定时" #2/#3 走查询确认流程。
3. **从模板填充** — 类型确定后，从 `examples.md` 取最接近的模板复制，只改值列表 / 自定义属性 / 类名；**禁止改 `value()/displayName()/help()` 方法签名**。
4. **接入模型字段（如需要）** — 单选 → `@Field.Enum` + 枚举类型；多选 → `@Field.Enum` + `List<E>` + `multi = true`。
5. **逐项核验「验收标准」清单** — 任一未达标返工对应步骤。

## 选择枚举类型

| 业务特征 | 类型 | 实现 | 值类型 |
|---------|------|------|-------|
| 单选状态/类型（最常见） | `IEnum<String>` | Java enum | String |
| 需要数值排序/计算 | `IEnum<Integer>` | Java enum | Integer |
| 多选标记 / 权限组合 | `BitEnum` | Java enum | Long（2 的幂） |
| 需要跨模块继承扩展 | `BaseEnum<E, String>` | Java class + `create()` | String |
| 继承扩展 + 多选 | `BaseEnum<E, Long>` + `BitEnum` | Java class + `create()` | Long（2 的幂） |
| 业务/系统错误码 | `ExpBaseEnum` + `@Errors` | Java enum | `(ERROR_TYPE, code, msg)` |

> 判别窍门：**多/单选** 看 DB 列是否同时承载多个值；**是否继承** 看下游模块是否会扩展枚举项；**错误码** 走独立的 `@Errors` 体系，不要混进 `@Dict`。

## Gotchas（已知陷阱，覆盖已踩过的全部坑）

| 陷阱 | 具体表现 | 应对 |
|------|---------|------|
| `value()` 上线后改了 | 历史数据 `status = "draft"`，代码改成 `value = "DRAFT_NEW"`，查询失配 | 增项安全；重命名/删除需评估数据迁移 |
| BitEnum 值不是 2 的幂 | `READ=1, WRITE=2, DELETE=3` → `READ|DELETE = 3 == WRITE`，按位组合崩溃 | 强制 `1L << n`：1, 2, 4, 8, 16… |
| BitEnum 字段写成 `EnumType` | 模型字段是单个枚举，DB 存不下多值 | 字段必须 `List<E>` + `multi = true`，DB 列 BIGINT |
| `@Field.Enum(dictionary = X)` 与类上 `@Dict` 不一致 | 平台用字段上的 dictionary，类上的失效，数据字典指向错乱 | `@Field.Enum` 不写 `dictionary`，由平台从字段类型推断 |
| `@Model` 字段手写 getter/Lombok | 覆盖 `get_d()/set_d()` 动态实现，字段读写失效 | 模型字段无 getter/setter、无 `@Data`；枚举类本身不受此限 |
| BaseEnum 写成 `enum BaseFoo extends BaseEnum` | Java enum 不能 extends 普通 class，编译报错 | BaseEnum 是 `class`：`public class Foo extends BaseEnum<Foo, T>`，加 `serialVersionUID`，用 `create()` 工厂方法定义项 |
| 构造函数 / `create()` 参数写反 | Java enum 是 `(value, displayName, help)`，BaseEnum 的 `create()` 是 `(name, value, displayName, help)`，把 name 漏了或位置写错 | 复制 `examples.md` 模板，不要凭记忆写签名 |
| BaseEnum 子类继承到不想要的枚举项 | 子类自动带入父类全部 enum 项，包含已废弃的 | 只想精选时用 `ref(ParentEnum.X)` 显式声明；自动继承是默认 |
| `DICTIONARY` 常量与别处重名 | 平台后注册的覆盖先注册的，数据字典 UI 显示错乱 | 先 `grep -r 'public static final String DICTIONARY'` 查现有命名空间 |
| 用 `appendMsg` 伪造错误码 | service 里 `throw new PamirsException().appendMsg("订单不存在")`，没有错误码枚举项 | 必须先在 ExpBaseEnum 里定义错误码枚举项，再 `new PamirsException(EnumItem)`。**在合法错误码上** `PamirsException.construct(EnumItem).appendMsg("订单号=xxx").errThrow()` 追加诊断上下文是允许的 |
| 错误码混进 `@Dict` 体系 | 用 `@Dict + IEnum<Integer>` 写错误码，没法被异常框架识别 | 错误码走 `@Errors + ExpBaseEnum`，三要素是 `(ERROR_TYPE, code, msg)` |
| 业务枚举加了 `@Base` | 系统级标记被业务枚举占用，启动可能冲突 | `@Base` 仅平台内置系统枚举用，业务枚举不要加 |
| JDK 17+ 语法泄漏 | 用了 `record` / `var` 在非 pamirs-ai-* 模块，构建失败 | 全项目（除 pamirs-ai-base / pamirs-ai-designer）按 JDK 8 语法写 |
| 在 PROXY 模型上 select 枚举字段 | `LambdaQueryWrapper.select(Foo::getStatus)` 渲染成 `` `null` AS `status` ``，MySQL 报 Unknown column | PROXY 模型字段非持久化，不能进 select 投影；参见 CLAUDE.md 投影限制 |

## 模型字段引用形态（速记）

```java
// 单选
@Field(displayName = "订单状态", defaultValue = "DRAFT")
@Field.Enum
private OrderStatusEnum status;

// 多选（BitEnum）
@Field(displayName = "权限", multi = true)
@Field.Enum
private List<PermissionEnum> permissions;
```

## 何时加载附加文件

- 需要注解参数表、接口方法签名、`Enums` 工具类 API、DB 存储类型对照 → 读 [`reference.md`](./reference.md)
- 需要某种枚举类型的完整模板（自定义属性、`ref()`、`switches()` 流式匹配、子类继承示例、错误码示例）→ 读 [`examples.md`](./examples.md)

## 不确定时

下列决策点不要替用户假设。调用 `AskUserQuestion`，给出推荐选项（先 grep 项目得出现有惯例作为推荐），允许自定义；苏格拉底式追问，一次 1–3 个核心问题，把方案的代价写进选项 description。

1. **枚举类型**（IEnum / BitEnum / BaseEnum / ExpBaseEnum）
2. **归属包**（先 grep 项目已有枚举位置，列已有路径作推荐）
3. **`DICTIONARY` 命名空间前缀**（先 grep 现有 `DICTIONARY` 常量，避免重名）
4. **枚举项清单**（每项的 `value` / `displayName` / `help`）
5. **自定义属性**（URL、图标、父分类、排序值等是否需要）
6. **模型字段引用 + 默认值**（是否接入模型 / `@Field.defaultValue`）
