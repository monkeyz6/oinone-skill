package {{packagePath}}.init.exports.{{bizDomain}};

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.file.api.context.ExcelDefinitionContext;
import pro.shushi.pamirs.file.api.enmu.ExcelTemplateTypeEnum;
import pro.shushi.pamirs.file.api.extpoint.AbstractExcelExportFetchDataExtPointImpl;
import pro.shushi.pamirs.file.api.extpoint.ExcelExportFetchDataExtPoint;
import pro.shushi.pamirs.file.api.model.ExcelExportTask;
import pro.shushi.pamirs.file.api.model.ExcelWorkbookDefinition;
import pro.shushi.pamirs.file.api.util.ExcelHelper;
import pro.shushi.pamirs.file.api.util.ExcelTemplateInit;
import pro.shushi.pamirs.framework.connectors.data.sql.query.QueryWrapper;
import pro.shushi.pamirs.meta.annotation.ExtPoint;
import pro.shushi.pamirs.meta.api.CommonApiFactory;
import pro.shushi.pamirs.user.api.behavior.impl.UserNameBehavior;
// TODO 替换：导入业务 Model / Action / AriesExportsUtils

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * {{templateDisplay}}
 */
@Component
public class {{ClassName}}ExportTemplate extends AbstractExcelExportFetchDataExtPointImpl
        implements ExcelTemplateInit, ExcelExportFetchDataExtPoint {

    public static final String TEMPLATE_NAME = "{{templateName}}";

    @Override
    public List<ExcelWorkbookDefinition> generator() {
        return Collections.singletonList(
                ExcelHelper.fixedHeader({{Model}}.MODEL_MODEL, TEMPLATE_NAME)
                        .createBlock(TEMPLATE_NAME, {{Model}}.MODEL_MODEL)
                        .setType(ExcelTemplateTypeEnum.EXPORT)
                        .addUnique({{Model}}.MODEL_MODEL, "code")
                        // === 字段定义 ===
                        .addColumn("fieldName", "字段显示名")
                        .addColumn("relation.name", "关联对象字段")
                        .addColumn("listField[*].subField", "集合子字段")
                        .build());
    }

    @ExtPoint.Implement(expression = "context.model == \"" + {{Model}}.MODEL_MODEL
            + "\" && context.name == \"" + TEMPLATE_NAME + "\"")
    @Override
    public List<Object> fetchExportData(ExcelExportTask exportTask, ExcelDefinitionContext context) {
        // 1. 分页查询全部数据
        List<{{Model}}> content = AriesExportsUtils.queryPageAll(page ->
                CommonApiFactory.getApi({{Action}}.class).queryPage(page,
                        (QueryWrapper<{{Model}}>) AriesExportsUtils.initWrapper(exportTask, context)));

        // 2. 加载关联字段（按需。框架不会自动加载关联对象）
        new {{Model}}().listFieldQuery(content, {{Model}}::getRelation);

        // 3. 用户名渲染（writeUserName / createUserName 字段）
        UserNameBehavior.set((Collection) content);

        return Collections.singletonList(content);
    }
}
