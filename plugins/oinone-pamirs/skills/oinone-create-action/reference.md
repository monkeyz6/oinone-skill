# Oinone Action & Function 完整注解参数与约束规则

## 类级别注解

### @Model.model（绑定模型）

Action/Function 类通过 `@Model.model` 绑定到目标模型。类必须同时加 `@Component`。

### @Fun（非模型类绑定）

当 Action/Function 定义在非模型类中时，使用 `@Fun` 指定目标模型编码。`@Fun` 也用于表达式函数等跨模型场景，此时 value 设为函数命名空间。

## Action 与 Function 的区别

| 特性 | Action | Function |
|------|--------|----------|
| 注解 | `@Action` + `@Action.Advanced`（可选） | `@Function` + `@Function.Advanced`（可选） |
| 前端可见 | 是，会渲染为按钮 | 否，仅后端调用（除非 openLevel 包含 API） |
| 绑定视图 | 通过 `bindingType` 指定 | 无视图绑定 |
| 上下文类型 | 支持 SINGLE/BATCH/CONTEXT_FREE | 无此概念 |
| 典型用途 | 按钮操作、表单提交、批量处理 | 数据查询、业务计算、内部逻辑 |

## @Action 注解详解

### @Action（方法级别）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `displayName` | String | "" | 按钮显示名称（必填） |
| `label` | String | "" | 按钮显示文字（优先于 displayName） |
| `summary` | String | "" | 描述 |
| `contextType` | ActionContextTypeEnum | SINGLE | 上下文类型 |
| `bindingType` | ViewTypeEnum[] | {TABLE} | 绑定的视图类型 |
| `mapping` | Prop[] | {} | 数据传输映射 DSL |
| `context` | Prop[] | {} | 上下文参数 |
| `attributes` | Prop[] | {} | 扩展属性 |

### @Action.Advanced（方法级别，可选）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | "" | 技术名称（默认取方法名） |
| `type` | FunctionTypeEnum[] | UPDATE | 函数类型（增删改查） |
| `args` | String[] | {} | 参数名列表 |
| `invisible` | String | "" | 显隐表达式 |
| `disable` | String | "" | 禁用表达式 |
| `rule` | String | "" | 服务端过滤表达式 |
| `check` | boolean | false | 是否校验 |
| `managed` | boolean | false | 是否数据库管理器函数 |
| `bindingView` | String | "" | 绑定指定视图名称 |
| `priority` | int | 100 | 优先级 |
| `language` | FunctionLanguageEnum | JAVA | 函数语言 |

### ActionContextTypeEnum（上下文类型）

| 枚举值 | 说明 | 使用场景 |
|--------|------|----------|
| `SINGLE` | 单行 | 操作单条数据（默认） |
| `BATCH` | 多行 | 仅在批量选择时出现 |
| `SINGLE_AND_BATCH` | 单行和多行 | 单条和批量都可操作 |
| `CONTEXT_FREE` | 上下文无关 | 不依赖选中数据，如"新建"按钮 |

### ViewTypeEnum（视图类型）

| 枚举值 | 说明 |
|--------|------|
| `TABLE` | 表格视图 |
| `FORM` | 表单视图 |
| `DETAIL` | 详情视图 |
| `SEARCH` | 搜索视图 |
| `GALLERY` | 画廊视图 |
| `KANBAN` | 看板视图 |
| `TREE` | 树视图 |
| `CALENDAR` | 日历视图 |
| `CHART` | 图表视图 |
| `CUSTOM` | 自定义视图 |

### FunctionTypeEnum（函数类型）

| 枚举值 | 值 | 说明 |
|--------|-----|------|
| `CREATE` | 1L | 新增 |
| `DELETE` | 2L | 删除 |
| `UPDATE` | 4L | 更新（默认） |
| `QUERY` | 8L | 查询 |

## @Function 注解详解

### @Function（方法级别）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | "" | API 名称（默认取方法名） |
| `scene` | FunctionSceneEnum[] | {} | 可执行场景 |
| `summary` | String | "" | 描述 |
| `openLevel` | FunctionOpenEnum[] | {LOCAL, REMOTE} | 开放级别 |

### @Function.Advanced（方法级别，可选）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `displayName` | String | "" | 显示名称 |
| `type` | FunctionTypeEnum[] | UPDATE | 函数类型 |
| `managed` | boolean | false | 是否数据库管理器函数 |
| `builtin` | boolean | false | 是否内置函数 |
| `check` | boolean | false | 是否校验 |
| `category` | FunctionCategoryEnum | OTHER | 函数分类 |
| `timeout` | int | 默认值 | 超时时间 |
| `retries` | int | 0 | 重试次数 |
| `isLongPolling` | boolean | false | 是否支持长轮询 |
| `longPollingKey` | String | "userId" | 长轮询唯一 key |
| `longPollingTimeout` | int | 1 | 长轮询超时（秒） |

### @Function.fun（方法级别，可选）

指定函数编码，不可更改，默认与方法名相同。用法：`@Function.fun("customFunctionCode")`

### FunctionOpenEnum（开放级别）

| 枚举值 | 说明 |
|--------|------|
| `LOCAL` | 本地调用 |
| `REMOTE` | 远程调用（Dubbo RPC） |
| `API` | 开放接口（可通过 GraphQL/HTTP 调用） |

### FunctionCategoryEnum（函数分类，常用值）

`INSERT_ONE`, `INSERT_BATCH`, `UPDATE_ONE`, `UPDATE_BATCH`, `DELETE_ONE`, `DELETE_BATCH`, `QUERY_ONE`, `QUERY_LIST`, `QUERY_PAGE`, `OTHER`, `AI_TOOL`

## ViewAction 详解（@UxRouteButton）

### @UxRoute 参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `model` | String | 目标模型编码 |
| `viewType` | ViewTypeEnum | 目标视图类型（TABLE/FORM/DETAIL） |
| `viewName` | String | 指定目标视图名称 |
| `load` | String | 自定义加载函数名（如 `construct`、`queryOne`） |
| `domain` | String | 前端过滤条件（用户可移除） |
| `filter` | String | 后端过滤条件（始终生效，用户不可见） |
| `title` | String | 导航标题 |

### 默认加载函数

| 目标视图 | 默认函数 | 说明 |
|----------|----------|------|
| Form（新建） | `construct` | 初始化新表单数据 |
| Form（编辑） | `queryOne` | 查询记录用于编辑 |
| Detail | `queryOne` | 查询记录用于展示 |
| Table | `queryPage` | 分页查询 |

## UrlAction 详解（@UxLinkButton）

### @UxLink 参数

| 参数 | 说明 |
|------|------|
| `value` | URL，支持 `${activeRecord.fieldName}` 表达式 |
| `openType` | 打开方式（`OPEN_WINDOW` 新窗口等） |
| `compute` | 动态 URL 计算函数名（优先于 `value`） |

## ClientAction 详解（@UxClientButton）

### 常用内置客户端函数

| 函数 ID | 说明 | 上下文类型 |
|---------|------|-----------|
| `$$internal_ValidateForm` | 表单数据校验 | CONTEXT_FREE |
| `$$internal_GotoListTableRouter` | 返回上一页 | CONTEXT_FREE |
| `$$internal_ReloadData` | 刷新当前页面/表格 | CONTEXT_FREE |
| `$$internal_DeleteOne` | 删除选中行 | SINGLE_AND_BATCH |
| `$$internal_DialogSubmit` | 提交弹窗数据 | SINGLE_AND_BATCH |
| `$$internal_DialogCancel` | 关闭弹窗 | CONTEXT_FREE |
| `$$internal_GotoListImportDialog` | 打开导入弹窗 | CONTEXT_FREE |
| `$$internal_GotoListExportDialog` | 打开导出弹窗 | SINGLE_AND_BATCH |
| `$$internal_BatchUpdate` | 批量修改 | SINGLE |
| `$$internal_CopyOne` | 复制选中行 | SINGLE |

## 扩展点（ExtPoint）详解

### 快捷接口

| 接口 | 触发时机 |
|------|----------|
| `CreateBeforeExtPoint` / `CreateAfterExtPoint` | 创建前/后 |
| `CreateBatchBeforeExtPoint` / `CreateBatchAfterExtPoint` | 批量创建前/后 |
| `UpdateBeforeExtPoint` / `UpdateAfterExtPoint` | 更新前/后 |
| `UpdateBatchBeforeExtPoint` / `UpdateBatchAfterExtPoint` | 批量更新前/后 |
| `DeleteBeforeExtPoint` / `DeleteAfterExtPoint` | 删除前/后 |
| `QueryOneBeforeExtPoint` / `QueryOneAfterExtPoint` | 单条查询前/后 |
| `QueryListBeforeExtPoint` / `QueryListAfterExtPoint` | 列表查询前/后 |
| `PageBeforeExtPoint` / `PageAfterExtPoint` | 分页查询前/后 |
| `CountBeforeExtPoint` | 计数前 |

### @ExtPoint.Implement 参数

| 参数 | 说明 |
|------|------|
| `expression` | 生效条件表达式，如 `"context.model == 'xxx'"` |
| `priority` | 执行优先级（越小越先执行，默认 100） |

### 自定义扩展点规则

1. 用 `@Ext(Model.class)` 标注接口，定义扩展点方法并加 `@ExtPoint`
2. 实现类加 `@Component` + `@Ext(Model.class)`，方法加 `@ExtPoint.Implement`
3. 调用方式：`Ext.run(ExtPointInterface::method, new Object[]{args})`

## Hook 拦截器详解

### @Hook 参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `module` | String[] | 按模块过滤 |
| `model` | String[] | 按模型编码过滤 |
| `fun` | String[] | 按函数名过滤 |
| `priority` | int | 执行优先级（越小越先执行，默认 100） |
| `active` | boolean | 是否激活 |

### Hook 规则

- `HookBefore`：`run(Object[] args)` 返回修改后的入参数组
- `HookAfter`：`run(Object[] args, Object result)` 返回修改后的结果
- 类需加 `@Component` 和 `@Model.model`

## 触发器（Trigger）详解

### TriggerConditionEnum

| 值 | 说明 |
|------|------|
| `ON_CREATE` | 数据创建时触发 |
| `ON_UPDATE` | 数据更新时触发 |
| `ON_DELETE` | 数据删除时触发 |

### Trigger 规则

- 触发器通过 RocketMQ 异步执行，不在同一事务中
- `name` 建议格式：`ModelModel + "#Trigger#" + eventName`，全局唯一
- 需同时加 `@Function` 和 `@Function.Advanced`

## 定时任务（@XSchedule）详解

### 规则

- Cron 格式：6 个必填字段（秒 分 时 日 月 周）+ 可选年份
- 定时任务函数必须无参数
- 需同时加 `@Function(openLevel = FunctionOpenEnum.LOCAL)` 和 `@Function.Advanced`

## 异步任务（@XAsync）详解

### @XAsync 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `displayName` | String | — | 任务显示名称 |
| `limitRetryNumber` | int | -1（无限） | 最大重试次数 |
| `nextRetryTimeValue` | int | 60 | 重试间隔 |
| `nextRetryTimeUnit` | TimeUnitEnum | SECOND | 重试间隔单位 |
| `delayTime` | int | 0 | 延迟执行时间 |
| `delayTimeUnit` | TimeUnitEnum | SECOND | 延迟时间单位 |

## 事务控制（@PamirsTransactional）详解

### @PamirsTransactional 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `propagation` | int | REQUIRED | 事务传播行为 |
| `isolation` | int | DEFAULT | 隔离级别 |
| `timeout` | int | -1 | 超时时间（秒） |
| `readOnly` | boolean | false | 是否只读 |
| `enableXa` | boolean | false | 是否启用分布式事务 |
| `rollbackFor` | Class[] | — | 触发回滚的异常类 |
| `noRollbackFor` | Class[] | — | 不触发回滚的异常类 |

## 入参与出参规范

### 基本规则

1. Action 的入参通常是绑定模型的实例（单行）或 `List<模型>`（批量）
2. Action 的返回值通常是同类型的模型实例或列表
3. Function 的入参和出参更灵活，可以是任意模型类型
4. 查询类 Function 常用 `Pagination<T>` 作为分页查询的入参和出参

### 入参模式

- 单个模型实例：`public DemoOrder action(DemoOrder data)`
- 批量模型列表：`public List<DemoOrder> action(List<DemoOrder> dataList)`
- 分页查询：`public Pagination<DemoOrder> query(Pagination<DemoOrder> page, IWrapper<DemoOrder> queryWrapper)`
- 使用传输模型：`public DemoOrder action(DemoOrder data, DemoQueryRequest request)`

## 常见约定和注意事项

1. 类必须加 `@Component`：Action/Function 类需要被 Spring 容器管理。
2. 模型绑定：类上必须有 `@Model.model` 或 `@Fun` 注解指定绑定的模型。
3. Action 默认绑定 TABLE 视图：如果不指定 `bindingType`，Action 默认出现在表格视图上。
4. Function 默认 LOCAL + REMOTE：如果不指定 `openLevel`，Function 默认可本地和远程调用，但不开放 API。需要前端调用时必须加 `FunctionOpenEnum.API`。
5. 函数类型要匹配：查询方法用 `FunctionTypeEnum.QUERY`，新增用 `CREATE`，修改用 `UPDATE`，删除用 `DELETE`。这影响平台的事务管理和权限控制。
6. 方法名即函数编码：默认情况下方法名就是函数编码，安装后不可变更。如需自定义可用 `@Function.fun("code")` 或 `@Action.Advanced(name = "code")`。
7. 返回值不能为 void：Action 和 Function 方法必须有返回值，通常返回操作后的模型实例。
8. JDK 语法约束：除 `pamirs-ai-base` 和 `pamirs-ai-designer` 外，所有模块必须使用 JDK 8 兼容语法。

## 表达式系统（invisible/disable/domain/filter）

### 内置数据变量

| 变量 | 说明 |
|------|------|
| `activeRecord` | 当前选中的数据记录 |
| `activeModel` | 当前模型 |
| `activeField` | 当前字段 |

### 内置上下文变量

| 变量 | 说明 |
|------|------|
| `context.module` | 当前模块 |
| `context.lang` | 当前语言 |
| `context.env` | 当前环境 |

### 常用业务上下文函数

`CURRENT_CORP_ID`、`CURRENT_UID`、`CURRENT_USER_NAME`、`CURRENT_USER`、`CURRENT_ROLE_IDS`、`CURRENT_PARTNER_ID`

### 表达式使用场景

- `invisible`：控制按钮显隐，值为 true 时隐藏
- `disable`：控制按钮禁用，值为 true 时禁用
- `domain`：前端过滤条件，用户可移除
- `filter`：后端过滤条件，始终生效，用户不可见
