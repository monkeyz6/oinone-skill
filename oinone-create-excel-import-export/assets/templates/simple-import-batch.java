package {{packagePath}}.init.imports.{{bizDomain}};

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import pro.shushi.pamirs.file.api.context.ExcelImportContext;
import pro.shushi.pamirs.file.api.enmu.ExcelTemplateTypeEnum;
import pro.shushi.pamirs.file.api.enmu.TaskMessageLevelEnum;
import pro.shushi.pamirs.file.api.extpoint.AbstractExcelImportDataExtPointImpl;
import pro.shushi.pamirs.file.api.extpoint.ExcelImportDataExtPoint;
import pro.shushi.pamirs.file.api.model.ExcelImportTask;
import pro.shushi.pamirs.file.api.model.ExcelWorkbookDefinition;
import pro.shushi.pamirs.file.api.util.ExcelHelper;
import pro.shushi.pamirs.file.api.util.ExcelTemplateInit;
import pro.shushi.pamirs.meta.annotation.Ext;
import pro.shushi.pamirs.meta.annotation.ExtPoint;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.Models;
// TODO 替换：导入业务 Model / StringUtils

import java.util.Collections;
import java.util.List;

/**
 * {{templateDisplay}}（批量处理）
 *
 * 使用场景：跨行去重、整体校验、批量保存。
 */
@Slf4j
@Component
@Ext(ExcelImportTask.class)
public class {{ClassName}}BatchImportTemplate
        extends AbstractExcelImportDataExtPointImpl<List<{{Model}}>>
        implements ExcelTemplateInit, ExcelImportDataExtPoint<List<{{Model}}>> {

    public static final String TEMPLATE_NAME = "{{templateName}}";

    @Override
    public List<ExcelWorkbookDefinition> generator() {
        return Collections.singletonList(
                ExcelHelper.fixedHeader({{Model}}.MODEL_MODEL, TEMPLATE_NAME)
                        .createBlock(TEMPLATE_NAME, {{Model}}.MODEL_MODEL)
                        .setType(ExcelTemplateTypeEnum.IMPORT)
                        .addColumn("code", "*编码")
                        .addColumn("name", "*名称")
                        .build()
                        .setEachImport(false));
    }

    @ExtPoint.Implement(expression = "importContext.definitionContext.model == \""
            + {{Model}}.MODEL_MODEL + "\" && importContext.definitionContext.name == \""
            + TEMPLATE_NAME + "\"")
    @Override
    public Boolean importData(ExcelImportContext importContext, List<{{Model}}> dataList) {
        ExcelImportTask importTask = importContext.getImportTask();
        if (CollectionUtils.isEmpty(dataList)) {
            importTask.addTaskMessage(TaskMessageLevelEnum.ERROR, "导入内容不能为空");
            return Boolean.FALSE;
        }

        // 逐行校验（行号 = 索引 + 3，简单模板表头占 2 行）
        boolean valid = true;
        for (int i = 0; i < dataList.size(); i++) {
            {{Model}} data = dataList.get(i);
            if (StringUtils.isEmpty(data.getCode())) {
                importTask.addTaskMessage(TaskMessageLevelEnum.ERROR,
                        "第" + (i + 3) + "行: 编码不能为空");
                valid = false;
            }
        }
        if (!valid) return Boolean.FALSE;

        // 批量保存
        Models.data().createOrUpdateBatch(dataList);
        return Boolean.TRUE;
    }
}
