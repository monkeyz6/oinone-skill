# Oinone 枚举参考文档

## 注解详解

### @Dict（类级别，必需）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dictionary` | String | （必填） | 数据字典编码，全局唯一 |
| `displayName` | String | "" | 显示名称 |
| `name` | String | "" | 技术名称 |
| `summary` | String | "" | 描述 |
| `type` | int | 1 | 类型 |

### IEnum\<T\> 接口方法

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `value()` | T | 存储值（持久化到数据库的值） |
| `displayName()` | String | 显示名称（前端展示） |
| `help()` | String | 帮助文本/描述 |
| `name()` | String | 枚举名称（默认取 Java enum name） |
| `attributes()` | Map | 扩展属性（默认 null） |

## BitEnum 接口

BitEnum 用于多选场景，值必须是 2 的幂次方，支持按位组合。

BitEnum 提供的位运算方法：

| 方法 | 说明 |
|------|------|
| `isBitIn(Long bits)` | 检查当前枚举值是否在 bits 中 |
| `setBitIn(Long bits)` | 将当前枚举值加入 bits |
| `unsetBitIn(Long bits)` | 从 bits 中移除当前枚举值 |
| `getBitPos()` | 获取位位置（log2 + 1） |
| `in(List<T> options)` | 检查是否在列表中 |
| `addTo(List<T> options)` | 加入列表 |
| `removeFrom(List<T> options)` | 从列表移除 |

### BitEnum 约束规则

- 值必须是 2 的幂：1, 2, 4, 8, 16...，以支持按位运算组合
- 数据库存储类型为 BIGINT
- 模型字段使用 `List<枚举类型>` + `multi = true`

## BaseEnum 详解

可继承枚举使用 Java class（非 enum）实现，通过 `create()` 静态工厂方法定义枚举项。子类可以继承父类的所有枚举值并添加新值。

### BaseEnum 的 create() 工厂方法

`create()` 是 `BaseEnum` 提供的静态工厂方法，常用签名：

```java
// 完整四参数（推荐）
public static <E extends BaseEnum> E create(String name, T value, String displayName, String help)

// 带扩展属性
public static <E extends BaseEnum> E create(String name, T value, String displayName, String help, Map<String, Object> attributes)
```

参数说明：
- `name` — 枚举项名称（对应 Java enum 的 name，通常与 `public static final` 字段名一致）
- `value` — 存储值（持久化到数据库）
- `displayName` — 显示名称
- `help` — 帮助文本

### BaseEnum 约束规则

- 必须声明 `private static final long serialVersionUID` 字段
- 使用 Java class（非 enum），不可用 Java enum 语法
- 子类自动继承父类所有枚举值，可通过 `ref()` 显式引用父类枚举值
- 参数顺序约定：`create(name, value, displayName, help)`，其中 `name` 通常与 `public static final` 字段名保持一致

### Java enum 与 BaseEnum 的选择

| 特性 | Java enum + IEnum | BaseEnum |
|------|-------------------|----------|
| 继承扩展 | 不支持（Java enum 不可继承） | 支持子类继承并扩展枚举值 |
| 编译时安全 | 强类型，编译时检查 | 运行时注册，编译时不检查值 |
| 定义方式 | 枚举常量 + 构造函数 | `create()` 静态工厂方法 |
| 序列化 | Java 内置 enum 序列化 | 自定义 `readResolve()` |
| 适用场景 | 值固定不变的场景 | 需要跨模块扩展的场景 |

### BaseEnum 高级用法

#### ref() — 引用父类枚举值

子类可通过 `ref()` 显式引用父类的枚举值，而非自动继承全部。

#### switches() / switchGet() — 流式匹配

BaseEnum 提供流式 switch/case API，替代 Java switch 语句。匹配策略：
- `caseValue()` — 按 `value()` 匹配（最常用）
- `caseName()` — 按 `name()` 匹配
- `caseEnum()` — 按 IEnum 的 `value()` 匹配

## ExpBaseEnum / @Errors 详解

错误码枚举是一种特殊的枚举类型，用于定义业务/系统错误码。使用 `@Errors` 注解（而非 `@Dict`），实现 `ExpBaseEnum` 接口。三要素为 `(ERROR_TYPE, code, msg)`。

### ERROR_TYPE 分类

| 类型 | 说明 |
|------|------|
| `SYSTEM_ERROR` | 系统错误 |
| `GENERIC_ERROR` | 普通错误 |
| `REMOTE_ERROR` | 远程错误 |
| `BIZ_ERROR` | 业务错误（最常用） |
| `SECURITY_ERROR` | 权限错误 |
| `DATA_ERROR` | 数据错误 |
| `LOGIC_ERROR` | 逻辑错误 |

错误码约定：全局唯一整数，建议按模块分段分配（如 1001xxxx 为订单模块）。

## 数据字典编码命名规范

格式：`模块命名空间.枚举类名`

命名规范：
- 前缀为模块命名空间（camelCase）
- 后缀为枚举类名
- 用点号分隔
- 建议在枚举类中定义为 `DICTIONARY` 常量

## 枚举值的存储方式

| 枚举类型 | 数据库存储 | 说明 |
|----------|-----------|------|
| `IEnum<String>` | VARCHAR | 存储 `value()` 返回的字符串 |
| `IEnum<Integer>` | INT | 存储 `value()` 返回的整数 |
| `BitEnum`（Java enum） | BIGINT | 存储多个枚举值的按位或结果 |
| `BaseEnum<E, String>` | VARCHAR | 与标准枚举相同 |
| `BaseEnum<E, Integer>` | INT | 与整数枚举相同 |
| `BaseEnum<E, Long>` + `BitEnum` | BIGINT | 与位运算枚举相同 |

## 在模型字段中引用枚举

### 单选枚举字段

使用 `@Field.Enum` + 枚举类型，适合 IEnum。

### 多选枚举字段（BitEnum）

BitEnum 类型的字段使用 `List<枚举>` 类型，需设置 `multi = true`。

### @Field.Enum 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dictionary` | String | "" | 数据字典编码（通常不需要，自动从字段类型推断） |
| `size` | int | 128 | 存储字符长度 |
| `limit` | int | -1 | 枚举选择数量限制 |

## Enums 工具类

`pro.shushi.pamirs.meta.common.enmu.Enums` 提供统一的枚举操作 API，同时支持 Java enum 和 BaseEnum：

| 方法 | 说明 |
|------|------|
| `Enums.getEnum(Class, name)` | 按 name 获取枚举实例 |
| `Enums.getEnumByValue(Class, value)` | 按 value 获取枚举实例 |
| `Enums.getEnumByDisplayName(Class, displayName)` | 按 displayName 获取枚举实例 |
| `Enums.getEnumList(Class)` | 获取所有枚举值列表 |
| `Enums.getEnumMap(Class)` | 获取 name → 枚举实例的 Map |
| `Enums.getNameByValue(Class, value)` | 根据 value 获取 name |
| `Enums.getDisplayNameByValue(Class, value)` | 根据 value 获取 displayName |

## 常见约定和注意事项

1. 三要素必备：每个枚举值必须包含 `value`（存储值）、`displayName`（显示名称）、`help`（帮助文本）。

2. DICTIONARY 常量：建议在枚举类中定义 `public static final String DICTIONARY = "模块名.枚举类名";`，并在 `@Dict` 中引用。

3. value 不可变更：枚举的 `value()` 值一旦持久化到数据库后不可修改，否则会导致数据不一致。

4. BitEnum 值必须是 2 的幂：BitEnum 的值必须是 1, 2, 4, 8, 16... 以支持按位运算组合。

5. 枚举字段不需要显式指定 dictionary：`@Field.Enum` 通常不需要设置 `dictionary` 参数，平台会自动从字段的 Java 类型推断对应的数据字典。

6. @Base 注解：系统级枚举（平台内置的）使用 `@Base` 标记，业务项目中的枚举通常不需要加 `@Base`。

7. 枚举构造函数参数顺序：约定为 `(value, displayName, help)`，如有自定义属性追加在后面。

8. BaseEnum 的 create() 参数顺序：约定为 `create(name, value, displayName, help)`，其中 `name` 通常与 `public static final` 字段名保持一致。

9. BaseEnum 必须声明 `serialVersionUID`：可继承枚举类必须包含 `private static final long serialVersionUID` 字段。

10. JDK 语法约束：除 `pamirs-ai-base` 和 `pamirs-ai-designer` 外，所有模块必须使用 JDK 8 兼容语法（禁止 `record`、`var` 等）。
