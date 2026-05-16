# 命名与注解规范

## 包路径

模板按业务域放在所在 Maven 子模块的 `init.{exports|imports}.{bizDomain}` 包下：

```
{project}.{modulePath}.init.{exports|imports}.{bizDomain}
```

`{modulePath}` 是模板所在 core 子模块的包前缀，**根据人设/模块决定，不是固定的**。常见组合：

| 项目          | 人设         | 典型 modulePath        | 完整示例                                                           |
|-------------|------------|----------------------|--------------------------------------------------------------|
| deli-b2b    | 平台运营     | `core.platform`      | `pro.shushi.deli.aries.core.platform.init.exports.item`        |
| deli-b2b    | 卖家         | `core.seller`        | `pro.shushi.deli.aries.core.seller.init.imports.trade`         |
| deli-b2b    | 经销商买家     | `core.retailer`      | `pro.shushi.deli.aries.core.retailer.init.imports.major`       |
| deli-b2b    | libra（看板） | `libra.view`         | `pro.shushi.deli.aries.libra.view.init.imports.libra`          |
| kailas-aries | 平台运营     | `boss.platform`      | `pro.shushi.kailas.aries.boss.platform.init.exports.settlement` |
| kailas-aries | 商户         | `boss.merchant`      | `pro.shushi.kailas.aries.boss.merchant.init.imports.major`     |

> 找不准的时候，去本业务域已有的模板里抄一个邻居的 package（`init.exports.{域}` 同级），保持一致就行。**不要凭空压扁成 `{project}.init.imports.xxx`**——真实代码里没有这种形态。

**bizDomain**：业务域目录名。

常用值：`major`、`item`、`trade`、`inventory`、`fund`、`settlement`、`promotion`、`logistics`、`customer`、`reverse`、`credit`、`rebate`、`config`。

## 类命名

| 用途   | 命名模式                                  | 示例                                   |
|------|---------------------------------------|--------------------------------------|
| 导出   | `{Persona}{ModelShortName}ExportTemplate` | `DeliAriesPlatformItemExportTemplate` |
| 导入（逐行） | `{Persona}{ModelShortName}ImportTemplate` | `AriesPlatformPartnerImportTemplate`  |
| 导入（批量） | `{Persona}{ModelShortName}BatchImportTemplate` | `AriesMerchantOrderBatchImportTemplate` |

**Persona 前缀**：

| 项目          | 角色          | Persona 前缀          |
|-------------|-------------|---------------------|
| deli-b2b    | 平台运营        | `DeliAriesPlatform` |
| deli-b2b    | 卖家（品牌方/经销商） | `DeliAriesSeller`   |
| kailas-aries | 平台运营        | `AriesPlatform`     |
| kailas-aries | 卖家         | `AriesMerchant`     |

> 不确定时，先看同业务域已有模板的命名再决定。

## TEMPLATE_NAME 常量

- 修饰符：`public static final String`
- 取值：中文描述。例：`"平台商品导出"`、`"运营平台合作关系导入"`、`"经销商订单批量导入"`

## 关键注解

| 注解                                    | 适用场景     | 说明                                   |
|---------------------------------------|----------|--------------------------------------|
| `@Component`                          | 所有模板     | Spring 组件注册                          |
| `@Ext(ExcelImportTask.class)`         | 仅导入模板    | 声明扩展点宿主类                             |
| `@ExtPoint.Implement(expression=...)` | 导入/导出扩展点 | 表达式匹配 模型 + 模板名                       |
| `@Slf4j`                              | 推荐       | 路径 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j`，**不要**用 Lombok 版本 |

## `@ExtPoint.Implement` expression 语法

**导出**：

```java
@ExtPoint.Implement(expression = "context.model == \"" + Model.MODEL_MODEL
        + "\" && context.name == \"" + TEMPLATE_NAME + "\"")
```

**导入**：

```java
@ExtPoint.Implement(expression = "importContext.definitionContext.model == \""
        + Model.MODEL_MODEL + "\" && importContext.definitionContext.name == \""
        + TEMPLATE_NAME + "\"")
```

差别：导出用 `context`，导入用 `importContext.definitionContext`。

## ExcelFixedHeadHelper 主要方法

`ExcelHelper.fixedHeader(...)` 返回的 builder：

| 方法                                                                       | 说明                            |
|--------------------------------------------------------------------------|-------------------------------|
| `setDisplayName(String)`                                                 | 模板显示名                         |
| `setType(ExcelTemplateTypeEnum)`                                         | `IMPORT` / `EXPORT` / `IMPORT_EXPORT` |
| `setEachImport(Boolean)`                                                 | `true`=逐行，`false`=批量          |
| `setMaxErrorLength(Integer)`                                             | 最大错误数                         |
| `setClearExportStyle(Boolean)`                                           | `true`=导出 CSV（清除样式）           |
| `setExcelMaxSupportLength(Integer)`                                      | Excel 最大导出行数                  |
| `setCsvMaxSupportLength(Integer)`                                        | CSV 最大导出行数                    |
| `setDomain(String)`                                                      | RSQL 域过滤表达式                   |
| `createSheet(String)`                                                    | 创建 Sheet                      |
| `createBlock(String model)`                                              | 创建数据块                         |
| `createBlock(String sheetName, String model)`                            | 同时创建 Sheet 和数据块               |
| `addColumn(String fieldKey, String label, String... examples)`           | 添加列（简单模式，自动推断类型）              |
| `addColumn(String fieldKey, ExcelCellDefinition, String... examples)`    | 添加列（显式 cell definition）       |
| `addUnique(String model, String... fields)`                              | 唯一键约束                         |
| `addUniques(String model)`                                               | 自动添加模型元数据中的唯一键                |
| `build()`                                                                | 返回 `ExcelWorkbookDefinition`  |

`build()` 之后还能链式：`.setEachImport(...)`、`.setHasErrorRollback(...)` 等。

## 项目共用工具类

| 工具                                                              | 用途                              |
|-----------------------------------------------------------------|---------------------------------|
| `AriesExportsUtils.queryPageAll(pageQueryFn)`                   | 分页查询全部数据                        |
| `AriesExportsUtils.initWrapper(exportTask, context)`            | 前端查询条件 → QueryWrapper           |
| `UserNameBehavior.set(Collection)`                              | userId 字段渲染为用户名                 |
| `kailas.aries.common.utils.ExcelHelper.getMapFromEnumClass(...)` | 仅 kailas 项目：枚举 → Map 自动构建下拉值    |
| `pro.shushi.pamirs.framework.common.utils.ObjectUtils.clone(T)` | 模型深拷贝（**不是** Apache/Spring 版本）  |

### `AriesExportsUtils` 唯一 FQN

`AriesExportsUtils` 只有一份，定义在 kailas-aries 的 common 模块里：

```java
import pro.shushi.kailas.aries.core.common.utils.AriesExportsUtils;
```

deli-b2b 通过依赖 kailas-aries common 间接使用同一个类——**两个项目都用这个 FQN**，不要凭包名直觉造个 `pro.shushi.deli.aries.*.AriesExportsUtils`，那种类不存在。

### 错误码枚举（`PamirsException.construct(...)`）

`ExpEnumerate` 是 Pamirs 框架默认的错误码枚举，但**每个业务域往往有自己的扩展**：

- 通用：`pro.shushi.pamirs.meta.common.exception.ExpEnumerate`
- deli-b2b major 域：`pro.shushi.deli.aries.major.api.enmu.DeliAriesMajorExpEnumerate`（示例，请按项目实际查）
- kailas-aries major 域：`pro.shushi.kailas.aries.major.api.enmu.AriesMajorExpEnumerate`（示例）

写代码时**优先用所在业务域的 `ExpEnumerate` 子枚举**；如果找不到，可以先用框架默认的 `ExpEnumerate.BIZ_ERROR`，待开发者按项目替换。
