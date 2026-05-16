# 样式 / 字体 / 类型枚举速查

## 单元格样式 ExcelStyleDefinition

通过 `StyleDefinitionBuilder` 或 `ExcelHelper.createDefaultStyle()` 构建。

| 属性                  | Builder 方法                | 类型                             | 常用值                                                                                                                      |
|---------------------|---------------------------|--------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| horizontalAlignment | `setHorizontalAlignment`  | `ExcelHorizontalAlignmentEnum` | `GENERAL`(默认), `LEFT`, `CENTER`, `RIGHT`, `FILL`, `JUSTIFY`, `CENTER_SELECTION`, `DISTRIBUTED`                            |
| verticalAlignment   | `setVerticalAlignment`    | `ExcelVerticalAlignmentEnum`   | `TOP`, `CENTER`, `BOTTOM`                                                                                                |
| fillBorderStyle     | `setFillBorderStyle`      | `ExcelBorderStyleEnum`         | `NONE`, `THIN`, `MEDIUM`, `THICK`, `DASHED`, `DOTTED`, `DOUBLE`, `HAIR`, `MEDIUM_DASHED`, `DASH_DOT`, `SLANTED_DASH_DOT` |
| topBorderStyle      | `setTopBorderStyle`       | `ExcelBorderStyleEnum`         | 同上                                                                                                                       |
| rightBorderStyle    | `setRightBorderStyle`     | `ExcelBorderStyleEnum`         | 同上                                                                                                                       |
| bottomBorderStyle   | `setBottomBorderStyle`    | `ExcelBorderStyleEnum`         | 同上                                                                                                                       |
| leftBorderStyle     | `setLeftBorderStyle`      | `ExcelBorderStyleEnum`         | 同上                                                                                                                       |
| fillBorderColor     | `setFillBorderColor`      | `Integer`                      | `0x000000`(黑)                                                                                                            |
| topBorderColor      | `setTopBorderColor`       | `Integer`                      | 同上                                                                                                                       |
| rightBorderColor    | `setRightBorderColor`     | `Integer`                      | 同上                                                                                                                       |
| bottomBorderColor   | `setBottomBorderColor`    | `Integer`                      | 同上                                                                                                                       |
| leftBorderColor     | `setLeftBorderColor`      | `Integer`                      | 同上                                                                                                                       |
| fillPatternType     | `setFillPatternType`      | `ExcelFillPatternTypeEnum`     | `NO_FILL`, `SOLID_FOREGROUND`(纯色), `FINE_DOTS`, `ALT_BARS`, `SPARSE_DOTS`, `THICK_HORZ_BANDS`, `THICK_VERT_BANDS`, `BIG_SPOTS`, `BRICKS` 等 |
| backgroundColor     | `setBackgroundColor`      | `Integer`                      | `0xFFFFFF`(白)                                                                                                            |
| foregroundColor     | `setForegroundColor`      | `Integer`                      | `0xFFFF00`(黄)，需配 `SOLID_FOREGROUND`                                                                                       |
| wrapText            | `setWrapText`             | `Boolean`                      | `true` / `false`(默认)                                                                                                     |
| shrinkToFit         | `setShrinkToFit`          | `Boolean`                      | `true` / `false`(默认)                                                                                                     |
| width               | `setWidth`                | `Integer`                      | `6000`（常用列宽），仅首行样式生效                                                                                                     |
| height              | `setHeight`               | `Integer`                      | `2600`（多行说明）、`400`（标准），仅首列样式生效                                                                                            |

### 背景色（POI 行为，必须组合）

```java
ExcelHelper.createDefaultStyle()
    .setFillPatternType(ExcelFillPatternTypeEnum.SOLID_FOREGROUND)
    .setForegroundColor(0xFFFF00);  // 黄
```

只设 `backgroundColor` 不生效。

## 字体 ExcelTypefaceDefinition

通过 `TypefaceDefinitionBuilder.newInstance()...build()` 构建。

| 属性           | Builder 方法       | 类型                  | 默认值     | 说明                                                      |
|--------------|------------------|---------------------|---------|---------------------------------------------------------|
| typeface     | `setTypeface`    | `ExcelTypefaceEnum` | `SONG`  | `SONG`(宋体), `REGULAR_SCRIPT`(楷体), `BOLDFACE`(黑体), `YAHEI`(微软雅黑) |
| typefaceName | `setTypefaceName`| `String`            | `null`  | 自定义字体名（优先级高于 typeface）                                  |
| size         | `setSize`        | `Integer`           | `11`    | 字号                                                      |
| bold         | `setBold`        | `Boolean`           | `false` | 加粗                                                      |
| italic       | `setItalic`      | `Boolean`           | `false` | 斜体                                                      |
| strikeout    | `setStrikeout`   | `Boolean`           | `false` | 删除线                                                     |
| color        | `setColor`       | `Integer`           | `0xfff` | 字体色。**`0xa` = 红（框架必填字段标记惯例）**                            |
| typeOffset   | `setTypeOffset`  | `ExcelTypeOffsetEnum`| `NORMAL`| 上标/下标                                                  |
| underline    | `setUnderline`   | `ExcelUnderlineEnum`| `NONE`  | 下划线                                                     |

## 值类型 ExcelValueTypeEnum

| 类型                 | 默认 format                  | 说明                  |
|--------------------|----------------------------|---------------------|
| `STRING`           | `null`                     | 文本（默认）              |
| `INTEGER`          | `"0"`                      | 整数                  |
| `NUMBER`           | `"0.00"`                   | 小数                  |
| `DATETIME`         | `"yyyy-MM-dd HH:mm:ss"`    | 日期时间                |
| `BOOLEAN`          | `{"true":"是","false":"否"}` | 布尔                  |
| `ENUMERATION`      | `null`（需手动设 JSON map）      | 枚举下拉                |
| `CALENDAR`         | `null`                     | 日历                  |
| `RICH_TEXT_STRING` | `RichTextFormat` JSON      | 富文本                 |
| `FORMULA`          | `null`                     | 公式                  |
| `OBJECT`           | `null`                     | 对象（关联字段自动填充时）       |
| `BIT`              | `null`                     | 二进制枚举               |
| `COMMENT`          | `null`                     | 备注                  |
| `HYPER_LINK`       | `null`                     | 超链接                 |

## 模板类型 ExcelTemplateTypeEnum

| 类型              | 用途         |
|-----------------|------------|
| `IMPORT`        | 仅导入        |
| `EXPORT`        | 仅导出        |
| `IMPORT_EXPORT` | 通用（一份模板双向）|

## 合并单元格 ExcelCellRangeDefinition

| 创建方式      | 示例                                | 说明                               |
|-----------|-----------------------------------|----------------------------------|
| 字符串坐标（推荐） | `.createMergeRange("A1:K1")`      | 直观易读                             |
| 数字坐标（已废弃） | `.createMergeRange(0, 0, 0, 10)`  | `(beginRow, endRow, beginCol, endCol)` |

可在 Block 或 Sheet 级别调用。
