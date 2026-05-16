# 复杂模板深入（WorkbookDefinitionBuilder）

适用于需要多表头、说明行、合并单元格、富文本、自定义样式的场景。

## 结构

复杂模板的表头由多个 Header 组成。**每个 Header 占据 Excel 的一行**（配置行虽然 `isConfig=true`，但仍占行号——不是给人填的，是给 Pamirs 解析时映射字段用的）。

```
Excel 第 1 行：配置行（isConfig=true，每个 cell 用 setField("xxx") 绑定字段）
Excel 第 2 行：说明行（一个 cell 跨整行，富文本/红色提示）
Excel 第 3 行：显示表头（用户可见，必填字段红色）
———— 以下是数据行 ————
Excel 第 4 行起：用户输入
```

> 数据从第 4 行起；错误行号 = `index + 4`。
> **`createMergeRange` 用 `"A2:...2"` 合并说明行（不是 `A1:...1` 合并配置行）。**

## blockRange 计算

`blockRange` 是 `"A1:终止列终止行"` 格式：

- **行数** = 配置行 + 说明行 + 显示表头行数（多表头则更多）
- **列数** = 字段数

例：3 行 × 11 列 → `"A1:K3"`。列超 26 用双字母 `AA`、`AB`。

代码：

```java
int columnCount = 11;
String blockRange = "A1:" + (char)('A' + columnCount - 1) + "3";
```

## createMergeRange

推荐字符串格式：

```java
.createMergeRange("A1:K1")
```

可在 Block 或 Sheet 级别。常用场景：说明行跨整行、分组表头。

## 富文本 RichTextFormat

```java
import pro.shushi.pamirs.core.common.CollectionHelper;
import pro.shushi.pamirs.file.api.builder.TypefaceDefinitionBuilder;
import pro.shushi.pamirs.file.api.format.RichTextFormat;

ExcelTypefaceDefinition boldFont = TypefaceDefinitionBuilder.newInstance()
        .setBold(Boolean.TRUE).build();
ExcelTypefaceDefinition redFont = TypefaceDefinitionBuilder.newInstance()
        .setBold(Boolean.TRUE).setColor(0xa).build();  // 0xa = 红

// 字段标签 "编码" 前 2 字符染红
.setType(ExcelValueTypeEnum.RICH_TEXT_STRING)
.setFormat(JSONArray.toJSONString(CollectionHelper.<RichTextFormat>newInstance()
    .add(new RichTextFormat(0, 2, redFont))
    .build()))
```

`new RichTextFormat(startIdx, endIdx, typefaceDefinition)`：

- `startIdx` / `endIdx` 是字符级索引
- 多段格式叠加：依次 `.add(...)` 即可

## 完整骨架

```java
@Component
public class XxxTemplate implements ExcelTemplateInit {

    public static final String TEMPLATE_NAME = "复杂模板";

    @Override
    public List<ExcelWorkbookDefinition> generator() {
        WorkbookDefinitionBuilder builder = WorkbookDefinitionBuilder
                .newInstance(Model.MODEL_MODEL, TEMPLATE_NAME)
                .setDisplayName("显示名");

        // 预定义字体
        ExcelTypefaceDefinition boldFont = TypefaceDefinitionBuilder.newInstance()
                .setBold(Boolean.TRUE).build();
        ExcelTypefaceDefinition redFont = TypefaceDefinitionBuilder.newInstance()
                .setBold(Boolean.TRUE).setColor(0xa).build();

        int columnCount = 11;
        String blockRange = "A1:" + (char)('A' + columnCount - 1) + "3";

        builder.setEachImport(true)
                .createSheet().setName("Sheet名")
                .createBlock(Model.MODEL_MODEL, ExcelAnalysisTypeEnum.FIXED_HEADER,
                        ExcelDirectionEnum.HORIZONTAL, blockRange)
                // 合并 Excel 第 2 行（说明行），不是第 1 行（配置行被字段映射占用）
                .createMergeRange("A2:" + (char)('A' + columnCount - 1) + "2")
                .setPresetNumber(10)

                // 第1行：配置行（isConfig=true，隐藏，仅字段映射）
                .createHeader()
                    .setStyleBuilder(ExcelHelper.createDefaultStyle())
                    .setIsConfig(Boolean.TRUE)
                    .createCell().setField("code")
                        .setStyleBuilder(ExcelHelper.createDefaultStyle().setWidth(6000)).and()
                    .createCell().setField("name")
                        .setStyleBuilder(ExcelHelper.createDefaultStyle().setWidth(6000)).and()
                    // ...每个字段一个 createCell
                .and()

                // 第2行：说明行（富文本，合并）
                .createHeader()
                    .setStyleBuilder(ExcelHelper.createDefaultStyle(v -> v.setBold(Boolean.TRUE))
                            .setWrapText(true)
                            .setVerticalAlignment(ExcelVerticalAlignmentEnum.TOP)
                            .setHeight(2600))
                    .createCell()
                        .setValue("1.红色为必填。\n2.编码相同则更新。\n3.请确保关联数据已存在。")
                        .setType(ExcelValueTypeEnum.RICH_TEXT_STRING)
                        .setFormat(JSONArray.toJSONString(CollectionHelper.<RichTextFormat>newInstance()
                                .add(new RichTextFormat(0, 42, boldFont))
                                .add(new RichTextFormat(0, 8, redFont))
                                .build()))
                    .and()
                    // 合并区域内剩余 columnCount-1 个空 cell 必须占位
                    .createCell().and()
                    // ... 重复至 columnCount-1 次
                .and()

                // 第3行：显示表头（必填字段红色标记）
                .createHeader()
                    .setStyleBuilder(ExcelHelper.createDefaultStyle(v -> v.setBold(Boolean.TRUE))
                            .setHorizontalAlignment(ExcelHorizontalAlignmentEnum.CENTER))
                    .createCell().setValue("编码")
                        .setType(ExcelValueTypeEnum.RICH_TEXT_STRING)
                        .setFormat(JSONArray.toJSONString(CollectionHelper.<RichTextFormat>newInstance()
                                .add(new RichTextFormat(0, 2, redFont))
                                .build()))
                    .and()
                    .createCell().setValue("名称")
                        .setType(ExcelValueTypeEnum.RICH_TEXT_STRING)
                        .setFormat(JSONArray.toJSONString(CollectionHelper.<RichTextFormat>newInstance()
                                .add(new RichTextFormat(0, 2, redFont))
                                .build()))
                    .and()
                    .createCell().setValue("备注").and();

        return Collections.singletonList(builder.build());
    }
}
```

## 关键约定

1. **顺序**：配置行 → 说明行 → 显示表头（顶部往下，对应 Excel 行 1/2/3）。
2. **配置行字段必填**：每个数据字段必须有对应的 `createCell().setField(...)`。
3. **合并区域内空 cell 占位**：合并行内剩余列必须用 `.createCell().and()` 占位，数量 = `columnCount - 1`。
4. **`isConfig=true`** 标记该 Header 是字段映射元数据。它**仍占 Excel 行号**——所以 `createMergeRange` 合并说明行用 `A2:...2`，数据从第 4 行起。
5. **`0xa` 红色** 是框架惯例，不是标准 RGB。
6. **width/height** 仅在首行/首列样式中生效。
