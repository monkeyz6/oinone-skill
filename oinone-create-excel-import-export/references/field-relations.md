# 关联字段引用与加载

## 关联字段引用语法

| 场景               | 语法                  | 示例                                              |
|------------------|---------------------|-------------------------------------------------|
| 关联对象单值（多对一）      | `field.subField`    | `creater.name`, `targetCurrency.code`           |
| 关联对象集合（导出用）      | `field[*].subField` | `ariesDetails[*].itemName`, `petShops[*].shopName` |
| 关联对象集合按索引（导入用）   | `field[N].subField` | `customerContracts[0].contractNo`               |

> 导入和导出对集合字段的语法不同：导出用 `[*]` 取整列，导入用 `[N]` 按行展开。

## 导出加载关联字段

框架**不会**自动加载关联对象。需要在 `fetchExportData` 内显式 `listFieldQuery`：

```java
@Override
public List<Object> fetchExportData(ExcelExportTask exportTask, ExcelDefinitionContext context) {
    // 1. 分页查询全部数据
    List<Model> content = AriesExportsUtils.queryPageAll(page ->
        CommonApiFactory.getApi(Action.class).queryPage(page,
            (QueryWrapper<Model>) AriesExportsUtils.initWrapper(exportTask, context)));

    // 2. 加载关联字段（每个关联字段一次调用）
    new Model().listFieldQuery(content, Model::getRelation1);
    new Model().listFieldQuery(content, Model::getRelation2);
    new Model().listFieldQuery(content, Model::getListRelation);

    // 3. 把 userId 字段渲染为用户名（writeUserName / createUserName）
    UserNameBehavior.set((Collection) content);

    return Collections.singletonList(content);
}
```

## 枚举下拉构建

### 方式 1：手动构造 Map

```java
Map<String, String> statusMap = new HashMap<>();
statusMap.put(StatusEnum.ENABLED.value(), StatusEnum.ENABLED.displayName());
statusMap.put(StatusEnum.DISABLED.value(), StatusEnum.DISABLED.displayName());

ExcelCellDefinition cell = new ExcelCellDefinition();
cell.setType(ExcelValueTypeEnum.ENUMERATION)
    .setValue("*状态")
    .setFormat(JSON.toJSONString(statusMap));
```

### 方式 2：kailas 项目自带工具（仅 kailas-aries-common）

```java
Map enumMap = pro.shushi.kailas.aries.common.utils.ExcelHelper.getMapFromEnumClass(StatusEnum.class);
```

### 方式 3：Boolean 下拉

```java
Map<Boolean, String> boolMap = new HashMap<>();
boolMap.put(true, "是");
boolMap.put(false, "否");
ExcelCellDefinition cell = new ExcelCellDefinition();
cell.setType(ExcelValueTypeEnum.ENUMERATION)
    .setValue("是否启用")
    .setFormat(JSON.toJSONString(boolMap));
```

## 框架自动推断（无需手动 cell definition）

如果只用 `addColumn(fieldKey, label)`，框架会按模型元数据：

- 字段为枚举 → 自动生成下拉
- 字段为日期 → 自动设置 `yyyy-MM-dd HH:mm:ss` 格式
- 字段为布尔 → 自动 `{"true":"是","false":"否"}`
- 字段为数字 → 自动整数/小数格式

**只在需要覆盖默认行为时**才传 `ExcelCellDefinition`。
