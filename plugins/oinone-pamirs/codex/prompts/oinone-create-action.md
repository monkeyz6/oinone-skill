
# Oinone Action / Function 创建

## 适用范围

本 skill 仅覆盖 **Action（4 类按钮）** 和 **Function（含 ExtPoint/Validation）**。识别到以下场景应改用对应 skill：

| 场景 | 切换到 |
|---|---|
| 函数前/后置拦截（`@Hook`） | `oinone-create-hook` |
| binlog 数据变更触发（`@Trigger`） | `oinone-create-trigger-task` |
| Cron 定时任务（`@XSchedule`） | `oinone-create-schedule-task` |
| 异步任务（`@XAsync` / `ExecuteTaskAction`） | `oinone-create-async-task` |

## 工作流

1. **定位目标模型**：grep 仓库中 `@Model.model(...)` 找到目标模型类。找不到 → 先用 `oinone-create-model` skill 建模型。
2. **澄清模糊需求**：使用 `AskUserQuestion`（**禁止纯文本提问**，禁止自行假设）。判断标准与默认值见 [关键决策](#关键决策)。
3. **生成代码**：套 [默认骨架](#默认骨架)，复杂场景查 [examples.md](./examples.md)。
4. **自检**：逐条勾选 [验收标准](#验收标准)；不放心的细节再翻 [Gotchas](#gotchas) 里的"为什么"。
5. **编译验证**：`mvn -pl <module> -am compile -DskipTests -s ~/.m2/settings-changsha.xml -Pshushi`（本项目不写单元测试）。

## 验收标准

代码生成完成后逐条勾选——任何一条不过都属未完成：

- [ ] **类加了 `@Component`** 且有 `@Model.model(...)` 或 `@Fun(...)` 绑定模型
- [ ] **入参命名命中项目硬性规则**：单行 Action 用 `data`，批量用 `dataList`，分页 Function 用 `page` + `queryWrapper`，普通 `QUERY` Function 入参用 `query`
- [ ] **没写默认值**：`contextType=SINGLE`、`bindingType={TABLE}`、`type=UPDATE`、`@Action.Advanced.name=方法名` 全部省略；空的 `@Action.Advanced` 整个删掉
- [ ] **Function 前端可调时显式加 `FunctionOpenEnum.API`**（默认 `LOCAL+REMOTE` 不开放给前端）
- [ ] **`type` 与方法语义匹配**：查询 `QUERY`、新增 `CREATE`、更新 `UPDATE`、删除 `DELETE`——查询用 `UPDATE` 会误开写事务
- [ ] **方法返回值不是 `void`**（Action/Function/Trigger/异步/定时一律有返回值，没结果就 `return data`）
- [ ] **编译通过**（步骤 5 的 mvn 命令）

## 默认骨架

**ServerAction（~80% 场景）**——所有默认值都不写出来：

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderAction {

    @Action(displayName = "提交订单")
    public DemoOrder submitOrder(DemoOrder data) {
        data.setStatus(OrderStatusEnum.SUBMITTED);
        data.updateById();
        return data;
    }
}
```

省略掉的默认值：`contextType=SINGLE`、`bindingType={TABLE}`、`@Action.Advanced.type=UPDATE`、`@Action.Advanced.name=方法名`。

**Function（前端可调用）**——必须显式加 `API`：

```java
@Function(openLevel = {FunctionOpenEnum.LOCAL, FunctionOpenEnum.REMOTE, FunctionOpenEnum.API})
@Function.Advanced(type = FunctionTypeEnum.QUERY)
public DemoOrder loadDetail(DemoOrder query) {
    return query.queryById();
}
```

注意 `QUERY` Function 入参命名为 `query`（项目硬性规则）。

其他类型（批量 Action、UxRouteButton 跳转、UxLinkButton 外链、UxClientButton 内置客户端动作、ExtPoint、Validation、分页查询）→ [examples.md](./examples.md)。

## 关键决策

仅在用户描述**触发对应关键词**时才追问；其他情况按默认值即可。

| 维度 | 默认 | 必须追问的触发词 | 追问选项 |
|---|---|---|---|
| Action 类型 | `@Action`（ServerAction） | "跳转/弹窗/抽屉/详情页" | `@UxRouteButton` 各场景 |
| | | "外部链接/打开网址" | `@UxLinkButton` |
| | | "刷新/校验表单/关闭弹窗" | `@UxClientButton` + 内置函数 ID |
| `contextType` | `SINGLE` | "批量/选中多条" | `BATCH` 或 `SINGLE_AND_BATCH` |
| | | "新建/导入/不依赖选中" | `CONTEXT_FREE` |
| `bindingType` | `{TABLE}` | "表单页/详情页操作" | `FORM` / `DETAIL` / 多视图 |
| `Function.openLevel` | `{LOCAL, REMOTE}` | "前端要调用/GraphQL/小程序调用" | 加 `API` |
| `Advanced.type` | `UPDATE` | 方法语义是查/增/删 | `QUERY` / `CREATE` / `DELETE` |
| 显隐 / 禁用 | 不加 | "草稿状态才能/已完成不能" | `invisible` / `disable` 表达式 |

> 苏格拉底式追问：每次 1–4 个问题，提供 2–4 个选项（首项加 `(Recommended)`），允许用户输入自定义。

## Gotchas

按本项目踩坑频率倒序，写代码时逐项核对：

- **方法名即函数编码，安装后不可改**。命名要业务化、短而准（`submitOrder` 而非 `doSubmitOrderUpdateAction`）。需要更友好的对外编码用 `@Function.fun("xxx")` 或 `@Action.Advanced(name="xxx")`。
- **入参命名是项目硬性规则**（CLAUDE.md）：单行 Action 用 `data`，批量用 `List<T> dataList`，分页 Function 用 `Pagination<T> page` + `IWrapper<T> queryWrapper`，普通 `QUERY` Function 入参用 `query`。错命名会让前端按钮调用对接出问题。
- **方法返回值不能为 void**。Action/Function/Trigger/异步/定时任务一律有返回值——没结果就返回入参。
- **类必须加 `@Component`**。漏了不会被框架扫描，运行时报"function not found"。
- **`@Function` 默认不开放给前端**（仅 `LOCAL + REMOTE`）。前端需调用必须显式加 `FunctionOpenEnum.API`，否则 GraphQL 报"接口不存在"。
- **`Advanced.type` 必须匹配语义**：`QUERY` 不开事务，`CREATE/UPDATE/DELETE` 走写事务。错配会导致只读方法误开写事务、写操作不回滚等问题。
- **不要写默认值**（CLAUDE.md "精准修改"原则）：`contextType=SINGLE`、`bindingType={TABLE}`、`type=UPDATE`、`@Action.Advanced.name=方法名`、`@Action.Advanced` 整个空注解——全部省略。
- **JDK 8 语法约束**：除 `pamirs-ai-base` 和 `pamirs-ai-designer` 外禁用 `var`、`Stream.toList()`、`switch` 表达式、`List.of()` 等 JDK 9+ 特性。
- **`@Model` 类不要手写 getter/setter 或加 Lombok `@Data`**（会覆盖框架的 `get_d()`/`set_d()`）。Action/Function 类本身不受此限。
- **深拷贝用 `pro.shushi.pamirs.framework.common.utils.ObjectUtils.clone(T)`**——不是 Apache 或 Spring 的 `BeanUtils.copyProperties`。
- **禁止循环内 DB 查询/RPC**（CLAUDE.md 性能规则）：批量场景必须 `wrapper.in(...)` 一次查回，转 `Map` 后内存查找。写入用 `batchService.create/update`。
- **`PamirsException` 错误码必须用 `ExpEnumerate` 枚举项**，禁止 `appendMsg(...)` 拼字符串绕过。
- **库存查询必须先把克隆 SKU 翻译成主 SKU**（CLAUDE.md "库存查询"章节）：`Long inventorySkuId = detail.getSaleSkuTplId() != null ? detail.getSaleSkuTplId() : detail.getSaleSkuId();`，否则多主体场景会漏查。
- **`LambdaQueryWrapper.select()` 不能投影非持久化字段**（`store=false`、关系字段、PROXY 模型字段），否则 MySQL 报 `Unknown column 'null' in 'field list'`。

## 常用 ClientAction 内置函数

`@UxClient` 的 value 取值（`bindingType` 和 `contextType` 的取值已在 [关键决策](#关键决策) 表里覆盖）：

| 函数 ID | 用途 |
|---|---|
| `$$internal_ValidateForm` | 表单校验 |
| `$$internal_ReloadData` | 刷新当前页/表格 |
| `$$internal_DialogSubmit` / `$$internal_DialogCancel` | 弹窗提交/取消 |
| `$$internal_GotoListTableRouter` | 返回上一页 |
| `$$internal_DeleteOne` | 前端删除选中行 |

完整列表见 [reference.md](./reference.md#clientaction-详解)。

## 进阶按需加载

- **完整注解参数表 / 所有枚举值 / 表达式系统（`activeRecord`、`CURRENT_*`、`domain` vs `filter`）** → [reference.md](./reference.md)
- **各场景代码模板**（批量 Action、`@UxRouteButton` 跳转新建/编辑/带过滤、`@UxLinkButton` 表达式 URL、ExtPoint 实现、Validation 表达式校验、分页查询、自定义 ExtPoint 接口） → [examples.md](./examples.md)
- **跨模型/独立类定义**：用 `@Fun(ModelClass.MODEL_MODEL)` 替代 `@Model.model`，见 examples.md "类级别注解"

---

## 补充参考资料

如需更详细的 API / 示例 / 模板资源，请使用 Read 工具读取以下文件（路径相对于 plugin 仓库根目录）：

- 文件: `plugins/oinone-pamirs/skills/oinone-create-action/examples.md`
- 文件: `plugins/oinone-pamirs/skills/oinone-create-action/reference.md`

> 在 Codex CLI 中，请在本 marketplace 仓库根目录启动 codex，相对路径方可解析；或将仓库路径作为前缀传入 Read 工具。
