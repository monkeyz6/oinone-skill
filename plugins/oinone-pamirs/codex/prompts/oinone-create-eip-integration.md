
# EIP 集成接口代码生成

在 oinone-pamirs 里调用外部 HTTP 接口用 `@Integrate` 注解声明式模式：定义 Service + EipConfig，方法体 `return null;`，框架 AOP 接管 HTTP。本 skill 覆盖 JSON 与 XML 双向。

## 验收标准（每次任务完成逐条自查）

代码层面（可静态判定）：

- [ ] Service 接口：`@Fun(FUN_NAMESPACE)` + 方法 `@Function`，返回 `EipResult<SuperMap>`
- [ ] EipConfig 类实现 `IEipAnnotationSingletonConfig<T>`，含 `schema` + `host` 字段，含 `construct()` 方法（`@Function` + `FunctionTypeEnum.QUERY`）
- [ ] Constants 类把 path 抽成 `public static final String`，**不直接**写在 `@Integrate.Advanced` 字面量里
- [ ] Service 实现类：`@Integrate(config=...)` + `@Integrate.Advanced(path=...)`，方法体**严格** `return null;` —— 一行多余代码都不能有
- [ ] 方法存在 token / signature 等"非业务参数"时，`@Integrate.RequestProcessor.finalResultKey` **必须**显式指向业务参数名
- [ ] EipConfig **无** getter/setter、**无** Lombok（Pamirs 框架自动注入）
- [ ] XML 模式：自定义 Serializable / InOutConverter 的 `@Fun` 必须用 `EipConfigurationConstant.FUNCTION_NAMESPACE`，**不要**自造命名空间
- [ ] 项目目录名沿用既有拼写 `thirid/`（不是 `third/`）—— 这是项目历史拼写，统一比纠正更重要

交互层面：

- [ ] 用户给的信息不足以填一个字段时，**优先**用 `// TODO: <说明>` 占位并写入 CHANGES.md checklist，**而不是**逐步追问

## 不做什么

- 站内调用本系统服务 → 普通 Spring / Pamirs Service
- 复杂 SOAP / JAXB POJO 映射（多命名空间、深嵌套 >3 层、严格 schema 校验）→ wsimport + 手动 HTTP（参考 `CxmlParserService`）
- 对外暴露内部能力（inbound）→ `oinone-create-open-api` skill
- 异步 / 定时 / 触发任务 → `oinone-create-async-task` / `oinone-create-schedule-task` / `oinone-create-trigger-task`

## 工作模式（决策框架，不是步骤清单）

agent 自行从需求里抽取以下变量。**只有当推断出多种合理解读时**才用 `AskUserQuestion` 澄清，不要为每个字段都开一轮对话。

| 变量 | 怎么定 |
|------|--------|
| 目标项目 | 用户明确指定 → 用；未指定且 `@Integrate` 实例数 deli-b2b=4、kailas=0 → 默认 deli-b2b-oversea，但若用户上下文显示在 kailas 模块工作则切换 |
| Vendor 名 | 取第三方系统的英文短名小写：FedEx→fedex / 金蝶→kingdee / 钉钉→dingtalk |
| 数据格式 | 用户报文/字段长得像 XML（含 `<tag>` 或 SOAP envelope）→ XML；其余 → JSON |
| XML 走 Approach A 还是 wsimport | 命名空间数 ≤ 1 且层级 ≤ 3 → A；否则告诉用户走 wsimport 并停止 |
| 信息粒度 | 用户能给 path + 字段 → 完整模式；只有大方向 → 骨架 + TODO |
| 认证 | Token in Header → `convertParams` 单条；AppKey+Secret / 自定义签名 → 独立 Auth 处理器函数；OAuth → 独立 token 方法 |

包路径前缀：

- deli-b2b-oversea → `pro.shushi.deli.aries.eip` ；类名前缀 `DeliAries{Vendor}`
- kailas-aries-oversea → `pro.shushi.kailas.aries.eip` ；类名前缀 `Aries{Vendor}` ；首次落地需先确认 `kailas-aries-eip-api`/`-eip-core` 模块已挂在 pom 里

产出物（带 `*` 按需）：

```
1   Service 接口        eip-api    @Fun + @Function
2   EipConfig 模型     eip-api    IEipAnnotationSingletonConfig
3   Constants 常量      eip-api    API 路径
4*  Transient DTO       eip-api    结构化请求体
5   Service 实现        eip-core   @Integrate，return null;
6*  Auth 函数           eip-core   认证处理器
7*  Exception 函数      eip-core   异常判定处理器
8*  XML Serializable    eip-core   XML→SuperMap
9*  XML InOutConverter  eip-core   SuperMap→XML
```

详细模板见 `references/EXAMPLES.md`；注解全部属性见 `references/REFERENCE.md`。

## 注解契约速查

```java
// JSON 通用
@Integrate(config = XXXEipConfig.class)
@Integrate.Advanced(path = XXXConstants.SOME_PATH)
@Integrate.RequestProcessor(finalResultKey = "request", convertParams = ...)  // 多参数时必填
@Integrate.ResponseProcessor(...)   // 可选
@Integrate.ExceptionProcessor(...)  // 可选

// XML 仅响应
@Integrate.ResponseProcessor(
    serializableFun = XxxXmlSerializable.FUNCTION_NAME,
    serializableNamespace = EipConfigurationConstant.FUNCTION_NAMESPACE)

// XML 双向：再加上
@Integrate.RequestProcessor(
    finalResultKey = "request",
    inOutConverterFun = XxxXmlInOutConverter.FUNCTION_NAME,
    inOutConverterNamespace = EipConfigurationConstant.FUNCTION_NAMESPACE)
```

`FUNCTION_NAME` 用框架前缀常量拼接，不要手写字面量：

```java
public static final String FUNCTION_NAME =
    EipConfigurationConstant.SERIALIZABLE_PREFIX + "DeliAriesXxxXmlSerializable" + "stringToSuperMap";
// 等价 "EIP_SERIALIZABLE_DeliAriesXxxXmlSerializable_stringToSuperMap"
```

## 已知陷阱

| 陷阱 | 怎么识别 | 应对 |
|------|---------|------|
| **`finalResultKey` 漏配（最高频）** | 方法有 2+ 参数，未设 `finalResultKey`；运行时第三方报"missing keyword X"或"unknown field token" | 任一非业务参数（token / sign / timestamp）出现就**显式**设 `finalResultKey` 指向业务参数名。`convertParams` 只复制不删除原参数 |
| XML 复杂度超阈值 | 多 `xmlns:` 前缀 / 嵌套 >3 / 要 schema 校验 | 不要硬上 `@Integrate`，告诉用户走 wsimport 路线并停止生成 |
| Config 缺 `construct()` | 启动期找不到单例配置，方法调用 NPE | 任何 Config 都保留 `construct()` 模板（`@Function` + `FunctionTypeEnum.QUERY`） |
| Auth/Exception 命名空间漏填 | 框架按默认命名空间找不到函数 | `authenticationProcessorNamespace` / `exceptionProcessorNamespace` 必须与处理函数 `@Fun` 一致 |
| kailas 首次落 EIP | 项目原本 0 个 `@Integrate` 实例 | 落地前确认 `kailas-aries-eip-api`/`-eip-core` 已在 pom，目录用项目既有 `thrid/` 拼写 |

## References

- `references/REFERENCE.md` — `@Integrate` / `Advanced` / `RequestProcessor` / `ResponseProcessor` / `ExceptionProcessor` 全部属性与默认值
- `references/EXAMPLES.md` — 4 套真实集成代码模板（JSON 简单 / Token in Header / OAuth / XML 双向）
- 上游 https://guide.oinone.top

---

## 补充参考资料

如需更详细的 API / 示例 / 模板资源，请使用 Read 工具读取以下文件（路径相对于 plugin 仓库根目录）：

- 目录: `plugins/oinone-pamirs/skills/oinone-create-eip-integration/references/`

> 在 Codex CLI 中，请在本 marketplace 仓库根目录启动 codex，相对路径方可解析；或将仓库路径作为前缀传入 Read 工具。
