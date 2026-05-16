package {{packagePath}}.init.imports.{{bizDomain}};

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.file.api.context.ExcelImportContext;
import pro.shushi.pamirs.file.api.enmu.ExcelTemplateTypeEnum;
import pro.shushi.pamirs.file.api.enmu.ExcelValueTypeEnum;
import pro.shushi.pamirs.file.api.enmu.TaskMessageLevelEnum;
import pro.shushi.pamirs.file.api.extpoint.AbstractExcelImportDataExtPointImpl;
import pro.shushi.pamirs.file.api.extpoint.ExcelImportDataExtPoint;
import pro.shushi.pamirs.file.api.model.ExcelCellDefinition;
import pro.shushi.pamirs.file.api.model.ExcelImportTask;
import pro.shushi.pamirs.file.api.model.ExcelWorkbookDefinition;
import pro.shushi.pamirs.file.api.util.ExcelHelper;
import pro.shushi.pamirs.file.api.util.ExcelTemplateInit;
import pro.shushi.pamirs.meta.annotation.Ext;
import pro.shushi.pamirs.meta.annotation.ExtPoint;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.common.exception.PamirsException;
// TODO 替换：导入业务 Model / StatusEnum / ExpEnumerate / StringUtils

import java.util.*;

/**
 * {{templateDisplay}}（逐行处理）
 */
@Slf4j
@Component
@Ext(ExcelImportTask.class)
public class {{ClassName}}ImportTemplate extends AbstractExcelImportDataExtPointImpl<{{Model}}>
        implements ExcelTemplateInit, ExcelImportDataExtPoint<{{Model}}> {

    public static final String TEMPLATE_NAME = "{{templateName}}";

    @Override
    public List<ExcelWorkbookDefinition> generator() {
        // === 枚举下拉（按需） ===
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put(StatusEnum.ENABLED.value(), StatusEnum.ENABLED.displayName());
        statusMap.put(StatusEnum.DISABLED.value(), StatusEnum.DISABLED.displayName());
        ExcelCellDefinition statusCell = new ExcelCellDefinition();
        statusCell.setType(ExcelValueTypeEnum.ENUMERATION)
                .setValue("*状态")
                .setFormat(JSON.toJSONString(statusMap));

        // === Boolean 下拉（按需） ===
        Map<Boolean, String> boolMap = new HashMap<>();
        boolMap.put(true, "是");
        boolMap.put(false, "否");
        ExcelCellDefinition boolCell = new ExcelCellDefinition();
        boolCell.setType(ExcelValueTypeEnum.ENUMERATION)
                .setValue("是否启用")
                .setFormat(JSON.toJSONString(boolMap));

        return Collections.singletonList(
                ExcelHelper.fixedHeader({{Model}}.MODEL_MODEL, TEMPLATE_NAME)
                        .createBlock(TEMPLATE_NAME, {{Model}}.MODEL_MODEL)
                        .setType(ExcelTemplateTypeEnum.IMPORT)
                        // 字段定义（"*" 前缀 = 必填）
                        .addColumn("code", "*编码")
                        .addColumn("name", "*名称")
                        .addColumn("status", statusCell)
                        .addColumn("enabled", boolCell)
                        .addColumn("relation.code", "关联编码")
                        .addColumn("remark", "备注")
                        .build()
                        .setEachImport(true));
    }

    @ExtPoint.Implement(expression = "importContext.definitionContext.model == \""
            + {{Model}}.MODEL_MODEL + "\" && importContext.definitionContext.name == \""
            + TEMPLATE_NAME + "\"")
    @Override
    public Boolean importData(ExcelImportContext importContext, {{Model}} data) {
        ExcelImportTask importTask = importContext.getImportTask();
        try {
            checkRequired(data);
            validateBusiness(data);
            saveData(data);
        } catch (PamirsException e) {
            log.error("导入业务异常", e);
            importTask.addTaskMessage(TaskMessageLevelEnum.ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("导入异常", e);
            importTask.addTaskMessage(TaskMessageLevelEnum.ERROR, "导入失败: " + e.getMessage());
        }
        return Boolean.TRUE;
    }

    private void checkRequired({{Model}} data) {
        if (StringUtils.isEmpty(data.getCode())) {
            throw PamirsException.construct(ExpEnumerate.BIZ_ERROR)
                    .appendMsg("必填字段_编码_没填").errThrow();
        }
        if (StringUtils.isEmpty(data.getName())) {
            throw PamirsException.construct(ExpEnumerate.BIZ_ERROR)
                    .appendMsg("必填字段_名称_没填").errThrow();
        }
    }

    private void validateBusiness({{Model}} data) {
        // TODO 业务校验：查重 / 关联数据存在性 / ...
    }

    private void saveData({{Model}} data) {
        // TODO 持久化：创建或更新
    }
}
