
# Oinone-Pamirs Excel 导入导出模板

为 oinone-pamirs 模型生成 Excel 导入/导出模板的 Java 代码。

## 适用范围

- **做**：基于 `ExcelTemplateInit` + `ExcelImportDataExtPoint` / `ExcelExportFetchDataExtPoint` 的模板代码
- **不做**：通用 POI/EasyExcel、yml/pom 改动、非 Pamirs 文件解析

---

## 决策清单（按顺序锁定，每一项都要有明确答案）

需求里没明说的，**必须用 `AskUserQuestion` 问**（不是纯文本提问，不是自己假设）：

1. **类型**：导入 / 导出 / 双向？
2. **目标模型**：完整类名 + `MODEL_MODEL` 常量？
3. **字段清单**：含哪些字段、哪些是关联（`relation.name`、`list[*].field`）？
4. **复杂度**：要不要多表头/说明行/样式/合并/枚举下拉？
5. **导入**：`eachImport` 逐行 vs 批量？唯一键？
6. **导出**：是否需要 `fetchExportData`？哪些关联字段要 `listFieldQuery`？

锁定后，按下表选模板并基于它写：

| 输入                                  | 选哪份模板                                          |
|-------------------------------------|------------------------------------------------|
| 标准导出，无样式/合并                          | `assets/templates/simple-export.java`           |
| 逐行导入（默认推荐）                          | `assets/templates/simple-import-row.java`       |
| 批量导入（跨行去重/整体校验/批量保存）                | `assets/templates/simple-import-batch.java`     |
| 多表头 / 说明行 / 合并 / 富文本 / 自定义样式        | `assets/templates/complex-template.java`        |

> 经验：94% 的需求落在 Simple 模式。Complex 模式只在用户明确要求多表头/合并/富文本时启用。

包路径与命名规范见 [`references/naming-conventions.md`](references/naming-conventions.md)。

---

## 必须记住的硬约定（踩过的坑）

| 陷阱                          | 具体表现                                                                      | 应对                                                                |
|-----------------------------|---------------------------------------------------------------------------|-------------------------------------------------------------------|
| **包路径压扁**                   | 写成 `pro.shushi.deli.aries.init.exports.item`（缺 `core.platform` 中间层）        | 真实 root 是 `{project}.{modulePath}.init.{exports/imports}.{域}`。modulePath 看模板所在 core 子模块（`core.platform`/`core.seller`/`boss.merchant`/`libra.view`），抄业务域邻居最稳。 |
| **AriesExportsUtils 凭直觉造**  | deli-b2b 模板里写 `pro.shushi.deli.aries.*.AriesExportsUtils`，类不存在             | 全局只有一个 FQN：`pro.shushi.kailas.aries.core.common.utils.AriesExportsUtils`，deli-b2b 也用这个。 |
| **关联字段空列**                   | 导出文件里 `brand.name` 是空                                                      | 框架不自动加载，在 `fetchExportData` 里手动 `new Model().listFieldQuery(content, Model::getBrand)`。 |
| **必填字段没标记**                  | 用户不知道哪些必填                                                                 | 字段 label 前加 `"*"`（如 `"*编码"`）。                                     |
| **手写 ExcelCellDefinition 冗余** | 给枚举/日期字段都写了 `setType(...)` + `setFormat(...)`                              | `addColumn(fieldKey, label)` 不传 cell 时框架按模型元数据自动推断；只在覆盖默认时手写。       |
| **复杂模板合并行号错位**              | 写成 `createMergeRange("A1:E1")`，结果合并的是配置行不是说明行                              | 3 行表头：**配置行 isConfig=true 仍占 Excel 第 1 行**（不是隐藏，只是元数据），说明行在第 2 行，所以 `createMergeRange("A2:...2")`。数据从第 4 行起，错误行号 = `index + 4`。 |
| **简单模板行号错位**                  | 把第 3 行的数据当成第 1 行报错                                                         | 简单模板 2 行表头，数据从第 3 行起，错误行号 = `index + 3`。                          |
| **背景色不显示**                    | 只设了 `backgroundColor`，Excel 打开是白色                                          | POI 行为：同时设 `fillPatternType = SOLID_FOREGROUND` + `foregroundColor`。 |
| **红色不是 0xFF0000**             | 给必填字段写 `setColor(0xFF0000)`，框架渲染不出红色                                       | 框架红色是 `0xa`（约定，不是标准 RGB）。                                          |
| **getter/setter 手写或 Lombok**   | 给 `@Model` 加 `@Data` 或手写 `getXxx`                                          | `@Model` 基类已经动态注入，手写或 Lombok 会盖掉框架行为。                              |
| **集合字段语法弄反**                  | 导入用 `[*]` 或导出用 `[0]`                                                       | 导出取整列用 `field[*].sub`；导入按行取索引用 `field[0].sub`。                     |
| **clone 用错包**                  | `org.apache.commons.lang3.ObjectUtils.clone()` 拿不到框架元数据                    | 必须用 `pro.shushi.pamirs.framework.common.utils.ObjectUtils.clone()`。   |

---

## 注解速查

| 注解                                    | 适用       | 说明                                                  |
|---------------------------------------|----------|-----------------------------------------------------|
| `@Component`                          | 所有模板     | Spring 注册                                          |
| `@Ext(ExcelImportTask.class)`         | 仅导入      | 声明扩展点宿主                                            |
| `@ExtPoint.Implement(expression=...)` | 导入/导出扩展点 | 见下方                                               |
| `@Slf4j`                              | 推荐       | 路径 `pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j`（**不是 Lombok**） |

`@ExtPoint.Implement` expression：

```java
// 导出
"context.model == \"" + Model.MODEL_MODEL + "\" && context.name == \"" + TEMPLATE_NAME + "\""
// 导入
"importContext.definitionContext.model == \"" + Model.MODEL_MODEL + "\" && importContext.definitionContext.name == \"" + TEMPLATE_NAME + "\""
```

---

## 按需加载的参考文档

只在遇到对应主题时打开（不要一次性全读）：

| 主题                                            | 文件                                                       |
|------------------------------------------------|----------------------------------------------------------|
| 包路径 modulePath 真实表 / 类名 / 注解 / 工具类 FQN          | [`references/naming-conventions.md`](references/naming-conventions.md) |
| 样式/字体/边框/值类型枚举速查                              | [`references/style-cheatsheet.md`](references/style-cheatsheet.md)     |
| 关联字段语法、`listFieldQuery` 用法、枚举下拉构建              | [`references/field-relations.md`](references/field-relations.md)       |
| 复杂 builder：blockRange / merge / RichText / isConfig | [`references/advanced-builder.md`](references/advanced-builder.md)     |
| 导入运行时：行号、错误回写、`eachImport` 语义                  | [`references/import-runtime.md`](references/import-runtime.md)         |

---

## 验收标准（交付前逐条自检）

- [ ] 包路径形如 `{project}.{modulePath}.init.{exports|imports}.{域}`，`modulePath` 与邻居模板一致
- [ ] 类名 = `{Persona}{Model}{Export|Import|BatchImport}Template`
- [ ] `TEMPLATE_NAME` 为 `public static final String`，值为中文
- [ ] `@Component` 在类上
- [ ] 导入还加了 `@Ext(ExcelImportTask.class)`
- [ ] `@ExtPoint.Implement` expression 与模型/模板名匹配（导入/导出 expression 形态不同）
- [ ] 必填字段 label 都以 `*` 开头
- [ ] 导出的关联字段都在 `fetchExportData` 里 `listFieldQuery` 加载了
- [ ] 类里没有手写 getter/setter，也没有 Lombok `@Data`
- [ ] `AriesExportsUtils` import 路径是 `pro.shushi.kailas.aries.core.common.utils.AriesExportsUtils`

---

## 补充参考资料

如需更详细的 API / 示例 / 模板资源，请使用 Read 工具读取以下文件（路径相对于 plugin 仓库根目录）：

- 目录: `plugins/oinone-pamirs/skills/oinone-create-excel-import-export/assets/`
- 目录: `plugins/oinone-pamirs/skills/oinone-create-excel-import-export/references/`

> 在 Codex CLI 中，请在本 marketplace 仓库根目录启动 codex，相对路径方可解析；或将仓库路径作为前缀传入 Read 工具。
