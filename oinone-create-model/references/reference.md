# Oinone 模型与字段定义 — 详细参考

## 模型类型详解

Oinone 支持四种模型类型，通过 `@Model.Advanced(type = ModelTypeEnum.XXX)` 指定：

| 类型 | 枚举值 | 说明 | 是否建表 | 典型基类 |
|------|--------|------|----------|----------|
| 存储模型 | `STORE` | 持久化到数据库，默认类型 | 是 | `IdModel`, `CodeModel`, `BizCodeModel` |
| 抽象模型 | `ABSTRACT` | 提供公共字段和行为，不能直接实例化 | 否 | `IdModel`（标记为 abstract） |
| 代理模型 | `PROXY` | 复用父模型的表，可扩展非存储字段和行为 | 否（复用父表） | 继承目标存储模型 |
| 传输模型 | `TRANSIENT` | 不持久化，用于数据传输/DTO | 否 | `TransientModel`, `IdTransientModel` |

## 模型继承体系

```
K2 → AbstractModel → BaseModel → IdModel → CodeModel → NameCodeModel
                                                      → VersionCodeModel (乐观锁+编码)
                                         → VersionModel (乐观锁)
                                         → BizIdModel (业务基类)
                                         → BizCodeModel (带编码的业务基类)
TransientModel → IdTransientModel
BaseRelation → IdRelation → CodeRelation (多对多中间表基类)
```

选择基类的原则：
- 需要自增 ID → `IdModel`
- 需要自增 ID + 自动编码 → `CodeModel`
- 需要自增 ID + 自动编码 + name 字段 → `NameCodeModel`
- 需要乐观锁 → `VersionModel`（仅 ID）或 `VersionCodeModel`（ID + 编码）
- 业务模型需要创建人/修改人等审计字段 → `BizCodeModel` 或 `BizIdModel`
- 数据传输对象 → `TransientModel` 或 `IdTransientModel`
- 多对多中间表 → `BaseRelation`、`IdRelation` 或 `CodeRelation`

## @Model 注解参数

### @Model（类级别，必需）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `displayName` / `value` | String | "" | 模型显示名称（必填） |
| `summary` | String | "" | 模型描述摘要 |
| `labelFields` | String[] | {} | 数据标题字段，用于前端展示引用时的标签 |
| `label` | String | "" | 数据标题格式 |

### @Model.model（类级别，必需）

模型编码，全局唯一，安装后不可变更。命名规范：`模块命名空间.模型类名`，如 `demo.DemoOrder`。

### @Model.Advanced（类级别，可选）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | ModelTypeEnum | STORE | 模型类型 |
| `table` | String | "" | 自定义逻辑表名 |
| `index` | String[] | {} | 索引/联合索引 |
| `unique` | String[] | {} | 唯一索引 |
| `ordering` | String | "" | 默认排序，如 `"createDate DESC, id DESC"` |
| `priority` | long | 100 | 优先级 |
| `managed` | boolean | true | 是否自动建表/更新表 |
| `name` | String | "" | API 名称 |
| `inherited` | String[] | {} | 继承的模型编码 |
| `unInheritedFields` | String[] | {} | 不从父类继承的字段 |
| `unInheritedFunctions` | String[] | {} | 不从父类继承的函数 |

### @Model.Code（类级别，可选）

用于自动编码生成，通常配合 `CodeModel` 使用：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `sequence` | String | （必填） | 序列名称 |
| `prefix` | String | "" | 编码前缀 |
| `suffix` | String | "" | 编码后缀 |
| `size` | int | 16 | 编码长度 |
| `step` | int | 1 | 步长 |
| `initial` | long | 1000L | 初始值 |
| `format` | String | "" | 格式化模板 |

### @Model.Persistence（类级别，可选）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `logicDelete` | boolean | true | 是否逻辑删除 |
| `charset` | CharsetEnum | UTF8MB4 | 字符集 |
| `collate` | CollationEnum | BIN | 排序规则 |

### @Model.Ds（类级别，可选）

指定模型使用的数据源，用法：`@Model.Ds("数据源名称")`。

### @Model.MultiTable（多表继承 — 父模型）

多表继承允许多个子模型共享一个父模型的定义，每个子模型各自建表，通过鉴别字段区分类型。

| 参数 | 类型 | 说明 |
|------|------|------|
| `typeField` | String | 鉴别字段名（该字段必须在模型中定义） |

### @Model.MultiTableInherited（多表继承 — 子模型）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | String | （必填） | 鉴别字段的值，用于区分子模型类型 |
| `redundancy` | boolean | true | 是否将父模型字段冗余到子表 |

### @Model.ChangeTableInherited（换表继承）

继承另一个模型的定义但使用不同的表，适合归档表等场景。无参数，直接标注在类上。

### @Model.Constraints（模型级约束）

在模型级别声明外键约束关系，参数通过 `@Model.Constraint` 配置：

| 参数 | 类型 | 说明 |
|------|------|------|
| `foreignKey` | String | 外键名称 |
| `relationFields` | String[] | 当前模型的关联字段 |
| `referenceClass` | Class | 目标模型类 |
| `referenceFields` | String[] | 目标模型的被关联字段 |
| `onUpdate` | OnCascadeEnum | 更新时的级联操作 |
| `onDelete` | OnCascadeEnum | 删除时的级联操作 |

### OnCascadeEnum（级联操作）

| 值 | 说明 |
|------|------|
| `NO_ACTION` | 无操作 |
| `SET_NULL` | 设为 null（默认） |
| `CASCADE` | 级联操作 |
| `RESTRICT` | 限制操作 |

### @Model.Static（静态模型）

标记模型为静态模型，不参与低代码/无代码管理。参数：`module`（所属模块常量）。

## @Field 注解参数

### @Field 主注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `displayName` / `value` | String | "" | 字段显示名称 |
| `summary` | String | "" | 字段描述 |
| `required` | boolean | false | 是否必填 |
| `invisible` | boolean | false | 是否不可见 |
| `immutable` | boolean | false | 是否不可变更 |
| `unique` | boolean | false | 是否唯一索引 |
| `index` | boolean | false | 是否普通索引 |
| `store` | NullableBoolEnum | NULL | 是否存储（NULL 表示跟随默认） |
| `multi` | boolean | false | 是否多值字段 |
| `defaultValue` | String | "" | 默认值 |
| `compute` | String | "" | 计算函数编码 |
| `serialize` | String | "NON" | 后端序列化方式 |
| `requestSerialize` | String | "NON" | 前端序列化方式 |
| `priority` | long | -1 | 字段优先级 |
| `translate` | boolean | false | 是否需要国际化翻译 |
| `track` | boolean | false | 是否追踪变更 |

### @Field.Advanced 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `columnDefinition` | String | "" | 自定义列定义，如 `"TEXT"`, `"MEDIUMTEXT"` |
| `column` | String | "" | 自定义数据库列名 |
| `name` | String | "" | API 名称 |
| `charset` | CharsetEnum | UTF8MB4 | 字段字符集 |
| `collation` | CollationEnum | BIN | 字段排序规则 |
| `sudo` | boolean | false | 是否绕过权限控制 |
| `copied` | boolean | true | 是否可复制 |
| `insertStrategy` | FieldStrategyEnum | DEFAULT | 插入策略 |
| `updateStrategy` | FieldStrategyEnum | DEFAULT | 更新策略 |

## 字段类型映射

### 基础字段类型注解

| 注解 | Java 类型 | 数据库类型 | 说明 |
|------|-----------|------------|------|
| `@Field.String` | `String` | VARCHAR(128) | 字符串，默认 size=128 |
| `@Field.Text` | `String` | TEXT | 大文本 |
| `@Field.Html` | `String` | TEXT | HTML 富文本 |
| `@Field.Integer` | `Integer` / `Long` | INT / BIGINT | 整数 |
| `@Field.Boolean` | `Boolean` | TINYINT(1) | 布尔 |
| `@Field.Float` | `BigDecimal` | DECIMAL(M,D) | 浮点数 |
| `@Field.Money` | `BigDecimal` | DECIMAL(65,6) | 金额，默认 M=65, D=6 |
| `@Field.Date` | `java.util.Date` | DATETIME | 日期时间 |
| `@Field.Enum` | 枚举类型 | VARCHAR / INT | 枚举 |
| `@Field.Binary` | `String` | VARCHAR | 二进制/文件引用 |

### @Field.String 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `size` | int | 128 | 字符串最大长度 |

### @Field.Float 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `M` | int | 65 | 总位数 |
| `D` | int | 6 | 小数位数 |

### @Field.Date 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | DateTypeEnum | DATETIME | 日期类型：`DATE`、`DATETIME`、`TIME` |
| `fraction` | int | 0 | 毫秒精度（0-6） |

### @Field.Sequence 参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `sequence` | String | 序列名称（必填） |
| `prefix` | String | 编码前缀 |
| `size` | int | 编码长度 |

## 关联关系详细规则

### @Field.Relation 参数详解

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `relationFields` | String[] | {} | 当前模型的关联字段 |
| `referenceFields` | String[] | {} | 目标模型的被关联字段 |
| `store` | boolean | true | 关联关系是否存储 |
| `domain` | String | "" | 前端筛选可选项的过滤条件 |
| `limit` | int | -1 | 关系数量限制 |
| `pageSize` | long | 20 | 查询每页个数 |

### @Field.many2many 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `through` | String | "" | 中间表模型编码（MODEL_MODEL） |
| `relationFields` | String[] | {} | 中间表中与当前模型关联的字段 |
| `referenceFields` | String[] | {} | 中间表中与目标模型关联的字段 |
| `limit` | int | -1 | 关系数量限制 |
| `pageSize` | long | 20 | 查询每页个数 |
| `ordering` | String | "" | 排序 |

### @Field.one2many 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `limit` | int | -1 | 关系数量限制 |
| `pageSize` | long | 20 | 查询每页个数 |
| `ordering` | String | "" | 排序 |

### 关联关系使用规则

- M2O 关联时，外键字段（如 `companyId`、`companyCode`）需要在模型中显式定义为独立字段。
- 虚拟关联（不存储）：在 `@Field.Relation` 上设置 `store = false`，同时在 `@Field` 上设置 `store = NullableBoolEnum.FALSE`。
- 级联操作通过 `@Field.one2one`、`@Field.Relation` 上的 `onUpdate` / `onDelete` 参数配置，或通过 `@Model.Constraints` 在模型级别声明。

## 高级特性详细参数

### @Field.Related（关联引用字段）

从关联模型中引用字段值，类似计算字段。

- `@Field.Related({"关联字段名", "目标字段名"})` — 引用关联对象的单个字段
- `@Field.Related(related = {"关联字段名", "目标字段名"})` — 引用多级关联路径

`@Field.Related.Internal` 控制是否存储：

| 参数 | 类型 | 说明 |
|------|------|------|
| `store` | boolean | 是否持久化到数据库（false = 每次查询动态计算） |

### @Field.Override（覆盖继承字段）

覆盖从父模型继承的关联字段配置，参数为要覆盖的字段名（String）。

### @Validation（字段校验）

支持两种方式：

| 参数 | 类型 | 说明 |
|------|------|------|
| `check` | String | 引用同模型中定义的 `@Function` 校验函数编码 |
| `ruleWithTips` | Validation.Rule[] | 表达式规则数组，每条规则包含 `value`（表达式）和 `error`（错误提示） |

### 字段序列化

序列化方式常量（`Field.serialize`）：

| 常量 | 说明 |
|------|------|
| `NON` | 不序列化（默认） |
| `JSON` | JSON 格式 |
| `XML` | XML 格式 |
| `COMMA` | 逗号分隔 |
| `DOT` | 点号分隔 |

- JSON 序列化：将复杂对象或列表序列化为单个数据库列存储，通常配合 `@Field.Advanced(columnDefinition = "TEXT")` 使用。
- COMMA 序列化：将列表以逗号分隔存储为字符串，适合简单字符串列表。
- 关联对象也可通过设置 `@Field.Relation(store = false)` + `serialize = Field.serialize.JSON` 实现非关系型存储。

### 索引定义

通过 `@Model.Advanced` 的 `index` 和 `unique` 参数定义：
- 单字段索引：`index = {"fieldName"}`
- 联合索引：`index = {"field1,field2"}` （逗号分隔的字段名表示联合索引）
- 唯一索引：`unique = {"code"}`

也可在 `@Field` 上直接设置 `index = true` 或 `unique = true`。

### 多表继承

- 父模型使用 `@Model.MultiTable(typeField = "xxx")` 标注，鉴别字段必须在父模型中定义。
- 子模型使用 `@Model.MultiTableInherited(type = "TYPE_VALUE")` 标注，每个子模型各自建表。
- `redundancy = true`（默认）时，父模型字段会冗余到子表中。

### 换表继承

使用 `@Model.ChangeTableInherited` 继承模型定义但使用独立的表，适合归档表等场景。子模型拥有与父模型相同的字段定义，但数据存储在独立的表中。

### 乐观锁

- 继承 `VersionModel` 或 `VersionCodeModel` 基类，自动包含 `optVersion` 字段。
- 也可手动在模型中添加 `@Field.Version` 注解的字段。

## 常见约定和注意事项

1. `MODEL_MODEL` 常量：每个模型必须定义 `public static final String MODEL_MODEL = "模块名.类名";`，格式为 `模块命名空间.模型类名`。

2. 模型编码不可变更：`@Model.model` 的值一旦安装到数据库后不可修改。

3. 字段命名：Java 字段使用 camelCase，平台自动映射为数据库下划线命名。

4. 关联字段的外键：M2O 关联时，外键字段（如 `companyId`、`companyCode`）需要在模型中显式定义为独立字段。

5. 存储模型默认逻辑删除：除非通过 `@Model.Persistence(logicDelete = false)` 关闭。

6. 代理模型不建表：代理模型复用父模型的表，只能添加 `store = NullableBoolEnum.FALSE` 的非存储字段。

7. 传输模型不持久化：继承 `TransientModel` 的模型不会创建数据库表。

8. 索引定义：通过 `@Model.Advanced(index = {"field1", "field2,field3"}, unique = {"code"})` 定义，逗号分隔的字段名表示联合索引。

9. 多表继承：父模型使用 `@Model.MultiTable(typeField = "xxx")`，子模型使用 `@Model.MultiTableInherited(type = "TYPE_VALUE")`。每个子模型各自建表，通过鉴别字段区分类型。

10. 换表继承：使用 `@Model.ChangeTableInherited` 继承模型定义但使用独立的表，适合归档表等场景。

11. @Field.Related 引用字段：从关联模型引用字段值时，需确认是否存储（`store`）。存储的引用字段会持久化到数据库，非存储的每次查询时动态计算。

12. @Validation 校验：支持 `check` 引用校验函数和 `ruleWithTips` 表达式规则两种方式。校验函数需要在同模型中定义为 `@Function`。

13. JDK 语法约束：除 `pamirs-ai-base` 和 `pamirs-ai-designer` 外，所有模块必须使用 JDK 8 兼容语法（禁止 `var`、switch 表达式、文本块、`record`、`stream.toList()` 等）。
