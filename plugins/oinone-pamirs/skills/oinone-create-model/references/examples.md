# Oinone 模型与字段定义 — 代码模板与示例

## 模型定义模板

### 存储模型（Store Model）

最常用的模型类型，持久化到数据库，继承 `CodeModel` 自动获得自增 ID 和编码字段。

```java
package pro.shushi.pamirs.demo.api.model;

import pro.shushi.pamirs.meta.annotation.Field;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.base.common.CodeModel;

@Model(displayName = "示例订单")
@Model.model(DemoOrder.MODEL_MODEL)
@Model.Code(sequence = "SEQ", prefix = "SO", size = 8)
public class DemoOrder extends CodeModel {

    public static final String MODEL_MODEL = "demo.DemoOrder";

    @Field(displayName = "订单名称", required = true)
    @Field.String(size = 256)
    private String name;

    @Field(displayName = "订单金额")
    @Field.Money
    private java.math.BigDecimal amount;
}
```

### 抽象模型（Abstract Model）

提供公共字段和行为的基类，不能直接实例化，不建表。

```java
@Model(displayName = "业务基础模型")
@Model.model(DemoBaseModel.MODEL_MODEL)
@Model.Advanced(type = ModelTypeEnum.ABSTRACT)
public abstract class DemoBaseModel extends IdModel {

    public static final String MODEL_MODEL = "demo.DemoBaseModel";

    @Field(displayName = "备注")
    @Field.Text
    private String remark;
}
```

### 代理模型（Proxy Model）

复用父模型的表，可扩展非存储字段和行为，不建新表。

```java
@Model(displayName = "订单代理")
@Model.model(DemoOrderProxy.MODEL_MODEL)
@Model.Advanced(type = ModelTypeEnum.PROXY)
public class DemoOrderProxy extends DemoOrder {

    public static final String MODEL_MODEL = "demo.DemoOrderProxy";

    // 可添加非存储字段
    @Field(displayName = "计算字段", store = NullableBoolEnum.FALSE)
    @Field.String
    private String computedField;
}
```

### 传输模型（Transport/Transient Model）

不持久化，用于数据传输/DTO，不建表。

```java
@Model(displayName = "查询条件")
@Model.model(DemoQueryRequest.MODEL_MODEL)
public class DemoQueryRequest extends TransientModel {

    public static final String MODEL_MODEL = "demo.DemoQueryRequest";

    @Field(displayName = "关键字")
    @Field.String
    private String keyword;

    @Field(displayName = "开始日期")
    @Field.Date
    private java.util.Date startDate;
}
```

## 基础字段类型示例

各种常用字段类型的完整写法示例。

```java
// 字符串（默认 size=128）
@Field(displayName = "名称", required = true)
@Field.String
private String name;

// 字符串（自定义长度）
@Field(displayName = "描述")
@Field.String(size = 512)
private String description;

// 大文本
@Field(displayName = "内容")
@Field.Text
private String content;

// HTML 富文本
@Field(displayName = "邮件内容")
@Field.Html
@Field.Advanced(columnDefinition = "MEDIUMTEXT")
private String body;

// 整数
@Field(displayName = "数量")
@Field.Integer
private Integer quantity;

// 长整数
@Field(displayName = "长整数")
@Field.Integer
private Long bigNumber;

// 布尔
@Field(displayName = "是否启用")
@Field.Boolean
private Boolean enabled;

// 浮点数（M=总位数, D=小数位数）
@Field(displayName = "比率")
@Field.Float(M = 10, D = 4)
private java.math.BigDecimal rate;

// 金额（默认 M=65, D=6）
@Field(displayName = "金额")
@Field.Money
private java.math.BigDecimal amount;

// 日期
@Field(displayName = "生日")
@Field.Date(type = DateTypeEnum.DATE)
private java.util.Date birthday;

// 日期时间（默认）
@Field(displayName = "创建时间")
@Field.Date
private java.util.Date createTime;

// 日期时间（毫秒精度）
@Field(displayName = "调用时间")
@Field.Date(fraction = 3)
private java.util.Date callTime;

// 枚举
@Field(displayName = "状态")
@Field.Enum
private StatusEnum status;

// 二进制/文件
@Field(displayName = "附件")
@Field.Binary
private String attachment;
```

## 特殊字段注解示例

### @Model.model 用法

```java
@Model.model("模块名.模型类名")
```

### @Field.Sequence（编码字段）

手动定义编码字段，通常由基类自动提供，特殊场景下可手动添加。

```java
@Field(displayName = "编码")
@Field.String
@Field.Sequence(sequence = "SEQ_NAME", prefix = "P", size = 8)
private String code;
```

### @Field.PrimaryKey（主键字段）

```java
@Field.PrimaryKey
@Field(displayName = "ID")
private Long id;
```

### @Field.Version（乐观锁字段）

手动定义乐观锁字段，也可直接继承 `VersionModel` 或 `VersionCodeModel` 基类自动获得。

```java
@Field.Version
@Field.Integer
@Field.Advanced(columnDefinition = "bigint(20) DEFAULT '0'")
@Field(displayName = "乐观锁", defaultValue = "0", required = true, invisible = true)
private Long optVersion;
```

### @Field.Related（关联引用字段）

从关联模型中引用字段值，类似计算字段。

```java
// 引用关联对象的单个字段
@Field.Related({"originPartner", "name"})
@Field(displayName = "主合作伙伴名称")
private String originPartnerName;

// 引用多级关联路径
@Field.Related(related = {"moduleDependencyList", "dependencyModule"})
@Field(summary = "依赖模块编码列表", serialize = "COMMA", invisible = true, store = NullableBoolEnum.TRUE)
private List<String> moduleDependencies;
```

`@Field.Related.Internal` 控制是否存储：

```java
@Field.Related.Internal(store = false)
@Field.Related({"defaultCategory"})
@Field(summary = "分类编码", invisible = true, store = NullableBoolEnum.TRUE)
private String category;
```

### @Field.Override（覆盖继承字段）

覆盖从父模型继承的关联字段配置。

```java
@Field.Override("fieldName")
@Field.Relation(relationFields = {"newRelField"}, referenceFields = {"id"})
@Field(displayName = "覆盖后的字段")
private TargetModel target;
```

### @Validation（字段校验）

```java
// 引用校验函数
@Validation(check = "checkOrderName")
@Field(displayName = "订单名称", required = true)
@Field.String
private String name;

// 表达式规则校验（在 Action 方法上使用）
@Validation(ruleWithTips = {
    @Validation.Rule(value = "!IS_BLANK(name)", error = "名称不能为空"),
    @Validation.Rule(value = "LEN(name) <= 100", error = "名称长度不能超过100")
})
@Action(displayName = "提交")
public DemoOrder submit(DemoOrder data) { ... }
```

## 关联关系字段示例

### 多对一（Many2One）

```java
// 基于业务字段关联
@Field.many2one
@Field.Relation(relationFields = {"companyCode"}, referenceFields = {"code"})
@Field(displayName = "所属公司")
private Company company;

// 基于 ID 关联
@Field.many2one
@Field.Relation(relationFields = {"companyId"}, referenceFields = {"id"})
@Field(displayName = "所属公司")
private Company company;

// 关联的外键字段需要在模型中定义
@Field(displayName = "公司编码")
@Field.String
private String companyCode;
```

### 一对多（One2Many）

```java
@Field.one2many
@Field.Relation(relationFields = {"code"}, referenceFields = {"departmentCode"})
@Field(displayName = "岗位列表")
private List<Position> positionList;
```

### 多对多（Many2Many）

主模型中的 M2M 字段定义：

```java
// 主模型中的 M2M 字段
@Field.many2many(
    through = EmployeeRelPosition.MODEL_MODEL,
    relationFields = {"employeeId"},
    referenceFields = {"positionId"}
)
@Field.Relation(relationFields = {"id"}, referenceFields = {"id"})
@Field(displayName = "岗位列表")
private List<Position> positions;
```

中间表模型定义：

```java
@Model(displayName = "员工岗位关系")
@Model.model(EmployeeRelPosition.MODEL_MODEL)
public class EmployeeRelPosition extends BaseRelation {

    public static final String MODEL_MODEL = "demo.EmployeeRelPosition";

    @Field.PrimaryKey
    @Field(displayName = "员工ID")
    private Long employeeId;

    @Field.PrimaryKey
    @Field(displayName = "岗位ID")
    private Long positionId;
}
```

复合键的 M2M 示例：

```java
// 主模型
@Field.many2many(
    through = "CompanyRelEmployee",
    relationFields = {"employeeType", "employeeCode"},
    referenceFields = {"companyType", "companyCode"}
)
@Field.Relation(
    relationFields = {"employeeType", "code"},
    referenceFields = {"companyType", "code"}
)
@Field(displayName = "公司列表")
private List<Company> companyList;
```

### 一对一（One2One）

```java
@Field.one2one
@Field.Relation(relationFields = {"configId"}, referenceFields = {"id"})
@Field(displayName = "配置详情")
private Config config;
```

非存储的一对一（虚拟关联）：

```java
@Field.one2one
@Field.Relation(store = false)
@Field(displayName = "关联对象", store = NullableBoolEnum.FALSE)
private RelatedModel related;
```

### 级联操作配置

在关联字段上配置级联操作：

```java
@Field.one2one(onUpdate = OnCascadeEnum.CASCADE, onDelete = OnCascadeEnum.SET_NULL)
@Field.Relation(relationFields = {"configId"}, referenceFields = {"id"})
@Field(displayName = "配置")
private Config config;
```

## 高级特性示例

### @Model.MultiTable（多表继承）

父模型定义，包含鉴别字段：

```java
@Model(displayName = "基础动作")
@Model.model(BaseAction.MODEL_MODEL)
@Model.MultiTable(typeField = "actionType")
public class BaseAction extends CodeModel {

    public static final String MODEL_MODEL = "demo.BaseAction";

    @Field(displayName = "动作类型", required = true)
    @Field.Enum
    private ActionTypeEnum actionType;
}
```

子模型定义，各自建表：

```java
@Model(displayName = "服务端动作")
@Model.model(ServerAction.MODEL_MODEL)
@Model.MultiTableInherited(type = "SERVER")
public class ServerAction extends BaseAction {

    public static final String MODEL_MODEL = "demo.ServerAction";

    @Field(displayName = "函数编码")
    @Field.String
    private String functionCode;
}
```

### @Model.ChangeTableInherited（换表继承）

继承父模型定义但使用独立的表，适合归档表等场景：

```java
@Model(displayName = "归档订单")
@Model.model(ArchivedOrder.MODEL_MODEL)
@Model.ChangeTableInherited
public class ArchivedOrder extends DemoOrder {

    public static final String MODEL_MODEL = "demo.ArchivedOrder";
}
```

### @Model.Constraints（模型级约束）

在模型级别声明外键约束关系：

```java
@Model.Constraints({
    @Model.Constraint(
        foreignKey = "company",
        relationFields = {"companyId"},
        referenceClass = Company.class,
        referenceFields = {"id"},
        onUpdate = OnCascadeEnum.SET_NULL,
        onDelete = OnCascadeEnum.SET_NULL
    )
})
public class DemoOrder extends CodeModel { ... }
```

### @Model.Static（静态模型）

标记模型为静态模型，不参与低代码/无代码管理：

```java
@Model.Static(module = ModuleConstants.MODULE_BASE)
@Model.model(WorkerNode.MODEL_MODEL)
@Model(displayName = "工作节点")
public class WorkerNode extends IdModel { ... }
```

## 字段序列化示例

### JSON 序列化

将复杂对象或列表序列化为单个数据库列存储：

```java
// 将关联对象序列化为 JSON 存储
@Field(displayName = "扩展信息", serialize = Field.serialize.JSON)
@Field.Advanced(columnDefinition = "TEXT")
private ExtInfo extInfo;

// 将列表序列化为 JSON 存储
@Field(displayName = "标签列表", serialize = Field.serialize.JSON)
@Field.Advanced(columnDefinition = "TEXT")
private List<String> tags;
```

### COMMA 序列化

将列表以逗号分隔存储为字符串：

```java
// 逗号分隔存储
@Field(displayName = "标签", serialize = Field.serialize.COMMA)
@Field.String(size = 1024)
private List<String> tags;
```

### 关联对象的 JSON 序列化（非关系型存储）

```java
@Field.many2one
@Field.Relation(store = false)
@Field(displayName = "配置", store = NullableBoolEnum.TRUE, serialize = Field.serialize.JSON)
@Field.Advanced(columnDefinition = "TEXT")
private Config config;
```
