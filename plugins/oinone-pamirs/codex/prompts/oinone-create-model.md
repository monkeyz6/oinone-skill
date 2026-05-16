
# Oinone 模型与字段定义

生成 Oinone/Pamirs 的 `@Model` Java 实体类，覆盖字段定义、关联关系、继承、乐观锁、自动编码。

## 验收标准

代码产出后，逐条自查（每条必须可判定为是/否）：

- [ ] 基类与需求匹配：要自动编码用 `CodeModel`/`NameCodeModel`/`BizCodeModel`/`VersionCodeModel`；不需要编码用 `IdModel`/`BizIdModel`/`VersionModel`；纯 DTO 用 `TransientModel`/`IdTransientModel`；多对多中间表用 `BaseRelation`/`IdRelation`/`CodeRelation`
- [ ] `MODEL_MODEL` 常量格式为 `命名空间.模型类名`（如 `demo.DemoOrder`），且 `@Model.model(XXX.MODEL_MODEL)` 引用它
- [ ] 所有 `@Field` 子注解的参数名都是真实存在的（见"已知陷阱"表中的幻觉参数清单）
- [ ] 字符串字段用 `@Field.String(size=N)`（**不是 `max`**）
- [ ] 整数字段同时有 `@Field` 和 `@Field.Integer`，金额用 `@Field.Money`，长文本用 `@Field.Text`
- [ ] 关联字段同时有方向注解（`@Field.many2one`/`one2many`/`many2many`/`one2one`）和 `@Field.Relation`
- [ ] 多对多有中间表模型（继承 `BaseRelation`），主模型用 `@Field.many2many(through=..., relationFields=..., referenceFields=...)` 引用
- [ ] **类上没有任何 Lombok 注解**（`@Data`/`@Getter`/`@Setter`/`@Builder` 等）
- [ ] **类内没有手写的 `getXxx()`/`setXxx()` 方法**
- [ ] 代理模型（`PROXY`）的字段不重复加 `store=NullableBoolEnum.FALSE`（默认即非存储）
- [ ] 持久化字段才会被引用到 `LambdaQueryWrapper.select(...)` 里
- [ ] JDK 8 兼容语法（不用 `var`、switch 表达式、文本块、`record`、`stream.toList()`）

## 不做什么

- 不写 Action / Function（用 oinone-create-action）
- 不写枚举 / 数据字典（用 oinone-create-enum）
- 不写拦截器 Hook（用 oinone-create-hook）
- 不为已存在但未变更的模型重写整个类——只生成新增/修改的字段片段
- 不修改 `@Model.model()` 的 MODEL_MODEL 值（**安装后不可变更**）

## 工作模式

### 关键决策点（不确定就用 AskUserQuestion 提问，禁止假设）

1. **模型类型**：STORE（默认，建表）/ ABSTRACT（公共基类）/ PROXY（复用父表）/ TRANSIENT（DTO）
2. **基类**：见上面"验收标准"第 1 条
3. **MODEL_MODEL 命名空间**：先 grep 项目已有 `MODEL_MODEL` 常量看现有前缀；新模块请用户给一个
4. **字段类型**：用户说"金额"用 `@Field.Money`、"长文本"用 `@Field.Text`、"编码"考虑 `@Field.Sequence`；不确定就问 Java 类型 + size/精度
5. **关联**：方向（M2O/O2M/M2M/O2O）、关联键（id vs 业务编码）、中间表是否已存在、是否虚拟关联（`store=false`）

### 模型类型与基类速查

| 模型类型 | 是否建表 | 典型基类 |
|---------|---------|---------|
| STORE（默认） | 是 | `IdModel`, `CodeModel`, `BizCodeModel`, `VersionCodeModel`, `NameCodeModel` |
| ABSTRACT | 否 | abstract 标记的 `IdModel` 子类 |
| PROXY | 否（复用父表） | 继承目标存储模型 |
| TRANSIENT | 否 | `TransientModel`, `IdTransientModel` |
| 多对多中间表 | 是（关系表） | `BaseRelation`, `IdRelation`, `CodeRelation` |

需要审计字段（创建人/修改人）→ 选 `BizXxx`；需要乐观锁 → 选 `VersionXxx`。

## 已知陷阱

下面这些**全部是 baseline LLM 实测会犯的错**（来自 eval iteration-1 + 项目历史踩坑）：

| 陷阱 | 错误表现 | 应对 |
|------|---------|------|
| `@Field.String` 用 `max` 参数 | `@Field.String(max=128)` | 用 `size`：`@Field.String(size=128)` |
| `@Field` 主注解上写 `max` | `@Field(max=500)` | 删除，长度放 `@Field.String(size=500)` |
| 中间表主键用 `@Field.PRIMARY` | `@Field.PRIMARY` | 用 `@Field.PrimaryKey`（注意大小写） |
| `@Field.many2many` 幻觉参数 | `targetRelationFields=...`/`targetReferenceFields=...` | 这两个参数**不存在**。只有 `through`、`relationFields`、`referenceFields`、`limit`、`pageSize`、`ordering` |
| `@Field.Sequence` 幻觉参数 | `paddingChar=...`/`paddingDirection=...`/`paddingLength=...` | 这些参数**不存在**。只有 `sequence`、`prefix`、`size`；要自动补零靠 `size` 控制总长 |
| 手写 getter/setter | 类内出现 `public XX getXxx(){...}` / `setXxx(...)` | **必须删除**——框架基类用 `get_d()/set_d()` 动态访问，手写会覆盖框架行为 |
| Lombok 注解 | `@Data`/`@Getter`/`@Setter`/`@Builder` 等 | **必须删除**。Lombok 仅限非 Pamirs POJO（如 EIP 第三方对接的纯 DTO） |
| PROXY 字段加 `store=false` | PROXY 模型字段重复声明 `store=NullableBoolEnum.FALSE` | 删除——PROXY 默认非持久化 |
| 错选基类用 `IdModel` 然后手写 `code` | 用户要自动编码，却选 `IdModel` + 手动 `@Field.Sequence` | 直接继承 `CodeModel` + `@Model.Code(prefix=..., size=...)` |
| `enabled` 这类布尔字段忘写子注解 | 只有 `@Field(displayName=...)` 没有 `@Field.Boolean` | 每个字段都要有"主注解 `@Field` + 类型子注解 `@Field.Boolean`/`@Field.Integer`/...""两个 |
| `LambdaQueryWrapper.select()` 引用非持久化字段 | 运行报错 `Unknown column 'null' in 'field list'` | `store=false`、`@Field.Relation(store=false)`、`one2*`/`many2*`、PROXY 字段都不能进 `select()` |
| 改 `MODEL_MODEL` 值 | 启动报错或数据丢失 | 安装到数据库后**不可变更**，一次想好命名 |
| 用 JDK 9+ 语法 | `var x = ...`、`switch (x) -> ...`、文本块 `"""..."""` | 项目锁定 JDK 8（除 `pamirs-ai-base`/`pamirs-ai-designer`） |

## 参考文档（按需加载）

- [references/reference.md](references/reference.md) — 完整注解参数表（`@Model.*` / `@Field.*` 所有子注解 + 参数 + 默认值）。在需要查特定参数语义、不常见注解（`@Model.MultiTable`、`@Model.Constraints`、`@Field.Related`、`@Field.Override`、`@Validation`）时读
- [references/examples.md](references/examples.md) — 完整代码示例（4 种模型类型 + 所有字段类型 + 4 种关联 + 多表继承 + 序列化）。在写多对多中间表、多表继承、`@Field.Related`、JSON/COMMA 序列化时读

---

## 补充参考资料

如需更详细的 API / 示例 / 模板资源，请使用 Read 工具读取以下文件（路径相对于 plugin 仓库根目录）：

- 目录: `plugins/oinone-pamirs/skills/oinone-create-model/references/`

> 在 Codex CLI 中，请在本 marketplace 仓库根目录启动 codex，相对路径方可解析；或将仓库路径作为前缀传入 Read 工具。
