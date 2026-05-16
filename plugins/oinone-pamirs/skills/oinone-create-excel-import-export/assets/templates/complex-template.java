package {{packagePath}}.init.imports.{{bizDomain}};

import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.core.common.CollectionHelper;
import pro.shushi.pamirs.file.api.builder.TypefaceDefinitionBuilder;
import pro.shushi.pamirs.file.api.builder.WorkbookDefinitionBuilder;
import pro.shushi.pamirs.file.api.enmu.*;
import pro.shushi.pamirs.file.api.format.RichTextFormat;
import pro.shushi.pamirs.file.api.model.ExcelTypefaceDefinition;
import pro.shushi.pamirs.file.api.model.ExcelWorkbookDefinition;
import pro.shushi.pamirs.file.api.util.ExcelHelper;
import pro.shushi.pamirs.file.api.util.ExcelTemplateInit;
// TODO 替换：导入业务 Model

import java.util.Collections;
import java.util.List;

/**
 * {{templateDisplay}}
 *
 * 复杂模板：3 行表头（配置行 + 说明行 + 显示表头），合并单元格，富文本。
 * 数据从第 4 行起；错误行号 = index + 4。
 */
@Component
public class {{ClassName}}Template implements ExcelTemplateInit {

    public static final String TEMPLATE_NAME = "{{templateName}}";

    @Override
    public List<ExcelWorkbookDefinition> generator() {
        WorkbookDefinitionBuilder builder = WorkbookDefinitionBuilder
                .newInstance({{Model}}.MODEL_MODEL, TEMPLATE_NAME)
                .setDisplayName("{{displayName}}");

        // 预定义字体
        ExcelTypefaceDefinition boldFont = TypefaceDefinitionBuilder.newInstance()
                .setBold(Boolean.TRUE).build();
        ExcelTypefaceDefinition redFont = TypefaceDefinitionBuilder.newInstance()
                .setBold(Boolean.TRUE).setColor(0xa).build();  // 0xa = 框架红

        int columnCount = {{columnCount}};
        // blockRange: A1 至 (列字母+总行数)。3 行表头 + N 列 → "A1:?3"。例 5 列 = "A1:E3"
        String blockRange = "A1:" + (char) ('A' + columnCount - 1) + "3";

        builder.setEachImport(true)
                .createSheet().setName("{{sheetName}}")
                .createBlock({{Model}}.MODEL_MODEL, ExcelAnalysisTypeEnum.FIXED_HEADER,
                        ExcelDirectionEnum.HORIZONTAL, blockRange)
                // 合并说明行（说明行是 Excel 第 2 行，配置行占了第 1 行）
                .createMergeRange("A2:" + (char) ('A' + columnCount - 1) + "2")
                .setPresetNumber(10)

                // === 第 1 行：配置行（isConfig=true，Excel 中隐藏，仅做字段映射） ===
                .createHeader()
                .setStyleBuilder(ExcelHelper.createDefaultStyle())
                .setIsConfig(Boolean.TRUE)
                .createCell().setField("code")
                .setStyleBuilder(ExcelHelper.createDefaultStyle().setWidth(6000)).and()
                .createCell().setField("name")
                .setStyleBuilder(ExcelHelper.createDefaultStyle().setWidth(6000)).and()
                // TODO 每个数据字段一个 createCell().setField("xxx").and()
                .and()

                // === 第 2 行：说明行（富文本，合并跨整行） ===
                .createHeader()
                .setStyleBuilder(ExcelHelper.createDefaultStyle(v -> v.setBold(Boolean.TRUE))
                        .setWrapText(true)
                        .setVerticalAlignment(ExcelVerticalAlignmentEnum.TOP)
                        .setHeight(2600))
                .createCell()
                .setValue("1.红色为必填字段。\n2.编码相同则更新已有数据。\n3.请确保关联数据已存在。")
                .setType(ExcelValueTypeEnum.RICH_TEXT_STRING)
                .setFormat(JSONArray.toJSONString(CollectionHelper.<RichTextFormat>newInstance()
                        .add(new RichTextFormat(0, 42, boldFont))
                        .add(new RichTextFormat(0, 8, redFont))
                        .build()))
                .and()
                // 合并区域内剩余列必须填充空 cell（数量 = columnCount - 1）
                .createCell().and()
                // TODO 重复 columnCount-1 次
                .and()

                // === 第 3 行：显示表头（用户可见，必填红色） ===
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
