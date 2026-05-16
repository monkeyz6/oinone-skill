
# Oinone 开放接口（@Open）创建

通过 `@Open` 注解将内部业务函数暴露为 REST API，供外部系统通过 EIP 网关调用。

**请求路径形态**：`POST openapi/pamirs/{path}?tenant=pamirs`
**响应包装**：固定使用 `OpenEipResult<T>`

## 不确定就问，不要假设

遇到任何关键信息缺失，必须用 `AskUserQuestion` 工具收集，禁止默认猜测后直接生成代码。

### 必须确认的决策点

1. **目标子项目** — `deli-b2b-oversea`（得力 F2B）/ `kailas-aries-oversea`（ZORO）
2. **接口业务用途** — 谁来调（ERP / 电商 / 物流 / …）、做什么（创建订单 / 查库存 / …）
3. **`@Open(path = ?)`** — 小驼峰方法语义，例如 `createOutTradeOrder`、`queryOrderStatus`
4. **HTTP 方法** — `post`（默认推荐，写操作）/ `get`（纯查询）/ 不指定（用框架默认 POST）
5. **认证策略** — 五选一，详见下表
6. **入参与返回字段** — 若用户未提供，骨架代码用 `// TODO` 标记，Response DTO 默认仅含 `resultCode`(S/E) + `resultMsg`

### 认证策略默认值与备选

> 默认推荐 **Token 验证**（项目当前 90% 接口采用）。所有内置策略 namespace 均为 `EipFunctionConstant.FUNCTION_NAMESPACE`。

| 选项 | 常量 | 何时选 |
|---|---|---|
| Token 验证（默认） | `DEFAULT_TOKEN_AUTHENTICATION_PROCESSOR_FUN` | 90% 场景 |
| 无加密 Token | `DEFAULT_NO_ENCRYPT_AUTHENTICATION_PROCESSOR_FUN` | 验证 accessToken 但不加密传输 |
| 加密/解密 | `DEFAULT_AUTHENTICATION_PROCESSOR_FUN` | 按 EipApplication 配置 |
| MD5 签名 | `DEFAULT_MD5_SIGNATURE_AUTHENTICATION_PROCESSOR_FUN` | 上游要求签名 |
| 自定义 | — | 需特殊鉴权逻辑，额外生成 `IEipAuthenticationProcessor` 实现类 |

## 产物清单

| # | 文件 | 模块 | 必要性 |
|---|---|---|---|
| 1 | 接口 `*EipService` | `*-eip-open-api` | 必须 |
| 2 | 实现类 `*EipServiceImpl` | `*-eip-core` | 必须 |
| 3 | Response DTO | `*-eip-open-api` | 必须 |
| 4 | Path 常量追加 | `*-common` | 建议 |
| 5 | 自定义认证处理器 | `*-eip-core` | 仅自定义认证 |
| 6 | EIP 初始化 BizInit | `*-eip-core` | 仅当目标模块不存在 |

## 执行流程

1. **收集信息** — 用 `AskUserQuestion` 走完上述决策点。
2. **加载模板** — 读取 [`references/reference.md`](references/reference.md) 拿到完整注解参数、import 清单与代码模板。
3. **参考真实代码** — 读取 [`references/examples.md`](references/examples.md) 对照项目里已有的两类实现（内置认证 / 自定义认证）。
4. **定位文件位置** — 见下方"包路径速查"。
5. **生成代码** — 接口 → DTO → 实现类 → Path 常量追加。
6. **检查 BizInit** — 确认目标模块已有 `EipResolver.resolver(...)`；deli-b2b 已有 `DeliAriesEipBizInit`，无需新建；kailas-aries 需自查。

## 包路径速查

### deli-b2b-oversea
```
接口         : pro.shushi.deli.aries.eip.open.api.api
实现类       : pro.shushi.deli.aries.eip.core.service[.子包]
Response DTO : pro.shushi.deli.aries.eip.open.api.tmodel
认证处理器   : pro.shushi.deli.aries.eip.core.service[.子包]
Path 常量    : pro.shushi.deli.aries.common.constant.DeliAriesOpenApiConstants
```

### kailas-aries-oversea
```
接口         : pro.shushi.kailas.aries.eip.open.api
实现类       : pro.shushi.kailas.aries.eip.core.open[.子包]
Response DTO : pro.shushi.kailas.aries.eip.open.tmodel
```

## 命名规范

| 元素 | 规则 | 示例 |
|---|---|---|
| 接口名 | `DeliAries{业务域}EipService` | `DeliAriesOutTradeOrderEipService` |
| 实现类名 | `{接口名}Impl` | `DeliAriesOutTradeOrderEipServiceImpl` |
| Response DTO | `DeliAries{业务域}Response` | `DeliAriesOutTradeOrderCommonResponse` |
| FUN_NAMESPACE | `deli.aries.eip.{接口类名}` | `deli.aries.eip.DeliAriesOutTradeOrderEipService` |
| Path 常量 | 大写下划线 | `CREATE_OUT_TRADE_ORDER` |
| `@Open(path)` | 小驼峰 | `createOutTradeOrder` |

## Gotchas（项目特定坑点）

> 这些是 LLM 凭通用知识容易踩的本项目坑，模板生成前请逐条核对。

- **`@Data` 来自 Pamirs，不是 Lombok。** Response DTO 必须用 `pro.shushi.pamirs.meta.annotation.fun.Data`。引入 `lombok.Data` 会污染框架字段访问。
- **方法签名固定两个参数**：`(IEipContext<SuperMap> context, ExtendedExchange exchange)`。`ExtendedExchange` 是 `org.apache.camel.ExtendedExchange`，不要漏。
- **返回类型必须是 `OpenEipResult<T>`**，构造用 `new OpenEipResult<>(response)`（成功）或 `OpenEipResult.error(code, msg)`（失败）。
- **请求体解析三选一**（见 reference.md "请求数据解析模式"）：整体 `JsonUtils.parseObject` / Map / `SuperMap.getIteration`。当上游字段大小写不固定时优先 Map 路线，按多个 key 兼容读取（见 examples.md 示例 2 ZORO 接口）。
- **Path 常量集中管理。** 不要在 `@Open(path = "literal")` 写裸字符串，统一引用常量类。
- **BizInit 不要新建**。`deli-b2b-oversea` 已有 `DeliAriesEipBizInit`，新增开放接口无需再造初始化类——`EipResolver.resolver(模块, null)` 会扫描整个模块。
- **认证策略 namespace 必须显式给** `EipFunctionConstant.FUNCTION_NAMESPACE`，缺省会注册不上。
- **EIP 子项目隔离**：本 skill 只覆盖 `@Open`（入站），如果用户描述的是"我们调外部"，立即停下并指向 `oinone-create-eip-integration` skill。

## 详细参考

- **完整注解参数 + 5 个代码模板** → [`references/reference.md`](references/reference.md)
- **项目内 3 个真实代码示例**（内置认证 / 自定义认证 / BizInit） → [`references/examples.md`](references/examples.md)

---

## 补充参考资料

如需更详细的 API / 示例 / 模板资源，请使用 Read 工具读取以下文件（路径相对于 plugin 仓库根目录）：

- 目录: `plugins/oinone-pamirs/skills/oinone-create-open-api/references/`

> 在 Codex CLI 中，请在本 marketplace 仓库根目录启动 codex，相对路径方可解析；或将仓库路径作为前缀传入 Read 工具。
