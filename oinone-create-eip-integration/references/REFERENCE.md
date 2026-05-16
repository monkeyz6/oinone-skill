# EIP 集成接口 — 详细参考

## @Integrate 注解完整属性

注解定义在 `pro.shushi.pamirs.eip.api.annotation.Integrate`，作用于方法级别（`ElementType.METHOD`）。

### @Integrate（必需）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `config` | `Class<?>` | **必填** | EipConfig 配置类，必须实现 `IEipAnnotationSingletonConfig` |
| `name` | `String` | `""` | 接口名称，默认取函数 displayName |

### @Integrate.Advanced（常用）

可标注在方法或类级别。方法级优先于类级。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `path` | `String` | `""` | 请求路径，拼接在 host 后。如 `/api/v1/orders` |
| `host` | `String` | `""` | 目标主机，通常由 Config 提供，此处可覆盖 |
| `schema` | `String` | `""` | 协议（http/https），通常由 Config 提供 |
| `httpMethod` | `String` | `""` | HTTP 方法，默认 `POST` |
| `dynamicProtocolCacheSize` | `int` | `-1` | 动态协议缓存大小，≥1 启用动态集成 |

最终 URI = `schema://host/path`

### @Integrate.RequestProcessor（按需）

处理请求参数的映射和转换。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `finalResultKey` | `String` | `""` | 请求体主数据的 key 名。当方法有多个参数时，指定哪个参数是请求体 |
| `convertParams` | `ConvertParam[]` | `{}` | 参数映射数组，将方法入参映射到 HTTP 请求的不同位置 |
| `authenticationProcessorFun` | `String` | `""` | 认证处理函数名 |
| `authenticationProcessorNamespace` | `String` | `""` | 认证处理函数的命名空间 |
| `inOutConverterFun` | `String` | `""` | 输入输出转换函数名 |
| `inOutConverterNamespace` | `String` | `""` | 输入输出转换函数命名空间 |
| `serializableFun` | `String` | `""` | 序列化函数名 |
| `serializableNamespace` | `String` | `""` | 序列化函数命名空间 |
| `deserializationFun` | `String` | `""` | 反序列化函数名 |
| `deserializationNamespace` | `String` | `""` | 反序列化函数命名空间 |
| `paramConverterCallbackFun` | `String` | `""` | 参数转换回调函数名 |
| `paramConverterCallbackNamespace` | `String` | `""` | 参数转换回调函数命名空间 |

#### `finalResultKey` 何时**必须**显式设置（重要 — 不要漏）

**默认行为**：不设 `finalResultKey` 时，框架会把方法的**所有非框架参数**按**参数名**平铺到请求体顶层 —— 即使某个参数已经被 `convertParams` 复制到 header / query，**它仍然会被序列化到 body 里**（`convertParams` 只复制，不从 body 删除）。

**必须设的场景**：方法签名是「主请求体 + 旁路参数（token / signature / appKey 等）」组合时，必须把 `finalResultKey` 指向**主请求体那个参数的参数名**。例如：

```java
public EipResult<SuperMap> pushXxx(XxxEipTransient request, String token) { return null; }
```
此时必须设 `finalResultKey = "request"`，否则发出去的 body 是 `{"request": {...业务报文...}, "token": "..."}`，旁路的 `token` 会污染 body 顶层，被对端按业务报文解析时产生奇怪的报错。

**反例（真实故障）**：ZORO `/pgo/startFlow/...` 接口缺 `finalResultKey="request"`，对端拒收报 `"No keyword: businessKey is provided."` —— 不是真的缺 businessKey，是 token 漏进 body 后整体解析失败、回了泛化错误。

**可不设的场景**：方法只有一个参数，或所有参数都被 `convertParams` 显式映射（不依赖默认平铺）。

### @Integrate.ResponseProcessor（按需）

处理响应参数的映射和转换。属性与 `RequestProcessor` 完全相同。

常见用途：将外部 API 返回的字段名映射为内部字段名。

```java
@Integrate.ResponseProcessor(
    convertParams = {
        @Integrate.ConvertParam(inParam = "access_token", outParam = "accessToken"),
    }
)
```

### @Integrate.ExceptionProcessor（按需）

处理异常判定。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `exceptionPredictFun` | `String` | `""` | 异常判定函数名 |
| `exceptionPredictNamespace` | `String` | `""` | 异常判定函数命名空间 |
| `errorMsg` | `String` | `""` | 错误消息字段名 |
| `errorCode` | `String` | `""` | 错误代码字段名（如 `"success"`、`"code"`） |

### @Integrate.ConvertParam（参数映射）

用于 `RequestProcessor` 和 `ResponseProcessor` 的 `convertParams` 数组中。

| 参数 | 类型 | 说明 |
|------|------|------|
| `inParam` | `String` | 输入参数名（方法参数名 或 响应字段名） |
| `outParam` | `String` | 输出位置（HTTP 上下文中的目标位置） |

**常用 outParam 映射目标：**
- `IEipContext.HEADER_PARAMS_KEY + ".fieldName"` — 放入 HTTP Header
- `IEipContext.QUERY_PARAMS_KEY + ".fieldName"` — 放入 URL Query 参数
- `"fieldName"` — 放入请求体或从响应体提取

`IEipContext` 来自 `pro.shushi.pamirs.eip.api.IEipContext`。

---

## IEipAnnotationSingletonConfig 接口规范

```java
import pro.shushi.pamirs.eip.api.annotation.IEipAnnotationSingletonConfig;
```

Config 类必须满足：
1. 继承 `IdModel`
2. 实现 `IEipAnnotationSingletonConfig<Self>`
3. 有 `@Model.model()` 注解
4. 有 `schema`（协议）和 `host`（域名）字段
5. 有 `construct()` 方法

### construct() 方法规范

```java
@Function(openLevel = FunctionOpenEnum.API, summary = "XXX配置构造")
@Function.Advanced(type = FunctionTypeEnum.QUERY)
public XxxEipConfig construct(XxxEipConfig config) {
    XxxEipConfig eipConfig = (config == null ? new XxxEipConfig() : config).singletonModel();
    if (eipConfig != null) {
        return eipConfig;
    }
    return null;
}
```

### 常见 Config 字段

| 字段 | 用途 | 必需 |
|------|------|------|
| `schema` | HTTP 协议（http/https），defaultValue="https" | 是 |
| `host` | 服务端域名或完整地址 | 是 |
| `username` / `password` | Basic Auth 认证 | 否 |
| `appKey` / `appSecret` | API Key 认证 | 否 |
| `token` | 静态 Token | 否 |
| `code` | 唯一编码标识 | 否 |

---

## Service 接口规范

```java
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.core.common.SuperMap;

@Fun(XxxEipDataService.FUN_NAMESPACE)
public interface XxxEipDataService {
    String FUN_NAMESPACE = "aries.eip.XxxEipDataService";

    @Function
    EipResult<SuperMap> methodName(参数...);
}
```

**规则：**
- `@Fun` 标注在接口级别，值为 `FUN_NAMESPACE` 常量
- `@Function` 标注在每个方法上
- 返回类型统一为 `EipResult<SuperMap>`
- 入参可以是：`TransientModel` 子类、`SuperMap`、基础类型（`String`、`Integer` 等）

---

## Service 实现类规范

```java
import org.springframework.stereotype.Service;  // 或 @Component
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.eip.api.annotation.Integrate;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.core.common.SuperMap;

@Slf4j
@Service
@Fun(XxxEipDataService.FUN_NAMESPACE)
public class XxxEipDataServiceImpl implements XxxEipDataService {

    @Integrate(config = XxxEipConfig.class)
    @Integrate.Advanced(path = XxxConstants.SOME_PATH)
    @Function
    @Override
    public EipResult<SuperMap> methodName(参数...) {
        return null;  // 必须 return null，框架通过 AOP 处理
    }
}
```

**规则：**
- `@Slf4j` 使用 Pamirs 版本：`pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j`
- `@Service` 或 `@Component` 注册到 Spring
- `@Fun` 的值与接口的 `FUN_NAMESPACE` 保持一致
- 方法体 **必须** `return null;`

---

## Auth 处理器规范

当需要自定义认证逻辑时（如动态获取 token 并放入 header）：

```java
import pro.shushi.pamirs.eip.api.IEipAuthenticationProcessor;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.core.common.SuperMap;
import org.apache.camel.ExtendedExchange;

@Fun(XxxAuthFunction.FUN_NAMESPACE)
@Component
public class XxxAuthFunction implements IEipAuthenticationProcessor<SuperMap> {
    public static final String FUN_NAMESPACE = "aries.eip.XxxAuthFunction";
    public static final String FUN = "xxxAuthentication";

    @Function
    @Function.fun(FUN)
    public Boolean xxxAuthentication(IEipContext<SuperMap> context, ExtendedExchange exchange) {
        return authentication(context, exchange);
    }

    @Override
    public boolean authentication(IEipContext<SuperMap> context, ExtendedExchange exchange) {
        // 获取 token 并放入 header
        context.putInterfaceContextValue(
            IEipContext.HEADER_PARAMS_KEY + ".Authorization", "Bearer " + token);
        return true;  // 返回 true 表示认证成功
    }
}
```

在实现类中引用：
```java
@Integrate.RequestProcessor(
    authenticationProcessorNamespace = XxxAuthFunction.FUN_NAMESPACE,
    authenticationProcessorFun = XxxAuthFunction.FUN
)
```

---

## Exception 判定函数规范

当需要自定义异常判定逻辑时：

```java
@Fun(XxxExceptionPredictFunction.FUN_NAMESPACE)
public class XxxExceptionPredictFunction {
    public static final String FUN_NAMESPACE = "aries.eip.XxxExceptionPredictFunction";
    public static final String FUN = "xxxExpFunction";

    @Function
    @Function.fun(FUN)
    public Boolean xxxExpFunction(IEipContext<SuperMap> context) {
        // 返回 true 表示发生了异常
        String code = StringHelper.valueOf(
            context.getExecutorContextValue(IEipContext.DEFAULT_ERROR_CODE_KEY));
        return !"200".equals(code);
    }
}
```

在实现类中引用：
```java
@Integrate.ExceptionProcessor(
    errorCode = "code",
    exceptionPredictFun = XxxExceptionPredictFunction.FUN,
    exceptionPredictNamespace = XxxExceptionPredictFunction.FUN_NAMESPACE
)
```

---

## XML 格式扩展

### 概述

EIP 框架默认使用 JSON 序列化（`DefaultJSONSerializable`）。对于返回 XML 格式的外部接口，需要自定义两个组件：

1. **XML Serializable** — 将 XML 响应字符串解析为 `SuperMap`（用于响应处理）
2. **XML InOutConverter** — 将 `SuperMap` 请求体转换为 XML 字符串（用于请求发送，按需）

两者通过 `@Integrate.ResponseProcessor` 和 `@Integrate.RequestProcessor` 的 `serializableFun` / `inOutConverterFun` 属性引用。

> **注意**：框架对纯 XML（`application/xml`）没有内置实现，只内置了 SOAP XML（`DefaultSoapSerializable`）。纯 XML 场景必须自定义 Serializable。

### 适用场景判断

| 场景 | 推荐方式 | 说明 |
|------|----------|------|
| 简单 XML（平铺、无命名空间、≤3 层） | **Approach A**: @Integrate + 自定义 Serializable/Converter | 保持声明式模式，享受框架 AOP、Auth、异常处理 |
| 复杂 XML（深层嵌套、多命名空间、需 JAXB POJO） | **Approach B**: 手动 HTTP，不使用 @Integrate | 参考项目中 `CxmlParserService` 模式 |
| 仅响应是 XML，请求是 JSON | 仅需 XML Serializable | 不需要 InOutConverter |
| 请求和响应都是 XML | 两者都需要 | Serializable + InOutConverter |

### XML Serializable 实现规范

继承 `AbstractSerializable`，重写 `stringToSuperMap(String)` 方法，使用 DOM 解析 XML。

```java
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipDeserialization;
import pro.shushi.pamirs.eip.api.IEipSerializable;
import pro.shushi.pamirs.eip.api.constant.EipConfigurationConstant;
import pro.shushi.pamirs.eip.api.serializable.AbstractSerializable;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Slf4j
@Component
@Fun(EipConfigurationConstant.FUNCTION_NAMESPACE)
public class XxxXmlSerializable extends AbstractSerializable
        implements IEipSerializable<SuperMap>, IEipDeserialization<SuperMap> {

    public static final String FUNCTION_NAME =
            EipConfigurationConstant.SERIALIZABLE_PREFIX
            + "XxxXmlSerializable" + "stringToSuperMap";

    @Override
    @Function.Advanced(displayName = "XXX XML反序列化")
    @Function.fun(FUNCTION_NAME)
    public SuperMap serializable(Object inObject) {
        return super.serializable(inObject);
    }

    @Override
    protected SuperMap stringToSuperMap(String s) {
        // TODO: 根据实际 XML 结构实现解析逻辑
    }
}
```

**关键规范：**
- `@Fun` 的值**必须**使用 `EipConfigurationConstant.FUNCTION_NAMESPACE`（等于 `"pamirs.eip.default.namespace"`）
- `FUNCTION_NAME` 遵循格式：`EipConfigurationConstant.SERIALIZABLE_PREFIX + "类名" + "stringToSuperMap"`，即 `"EIP_SERIALIZABLE_" + "类名" + "stringToSuperMap"`
- 继承 `AbstractSerializable`，重写 `stringToSuperMap(String)` — 基类已处理 InputStream → String 的转换
- XML 解析推荐使用 DOM（`javax.xml.parsers.DocumentBuilder`），项目中 `DeliOaServiceImpl` 已有先例
- 参考项目中 `AriesKingdeeSerializable` 的注解模式（它是自定义 JSON 解析，注解结构完全一致）

### XML InOutConverter 实现规范

继承 `DefaultInOutConverter`，重写 `exchangeObject(ExtendedExchange, Object)` 方法，将 SuperMap 转为 XML 字符串。

```java
import org.apache.camel.ExtendedExchange;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.constant.EipConfigurationConstant;
import pro.shushi.pamirs.eip.api.converter.DefaultInOutConverter;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

@Slf4j
@Component
@Fun(EipConfigurationConstant.FUNCTION_NAMESPACE)
public class XxxXmlInOutConverter extends DefaultInOutConverter {

    public static final String FUNCTION_NAME =
            EipConfigurationConstant.IN_OUT_CONVERTER_PREFIX
            + "XxxXmlInOutConverter_" + "exchangeObject";

    @Override
    @Function.fun(FUNCTION_NAME)
    @Function.Advanced(displayName = "XXX XML请求体转换")
    public Object exchangeObject(ExtendedExchange exchange, Object inObject) throws Exception {
        // 设置 Content-Type
        exchange.getIn().setHeader("Content-Type", "application/xml; charset=UTF-8");

        SuperMap body = (SuperMap) inObject;
        // TODO: 根据实际请求结构构建 XML 字符串
        return xmlString;
    }
}
```

**关键规范：**
- `@Fun` 的值**必须**使用 `EipConfigurationConstant.FUNCTION_NAMESPACE`
- `FUNCTION_NAME` 遵循格式：`EipConfigurationConstant.IN_OUT_CONVERTER_PREFIX + "类名_" + "exchangeObject"`，即 `"EIP_IN_OUT_CONVERTER_" + "类名_" + "exchangeObject"`
- 继承 `DefaultInOutConverter`，重写 `exchangeObject` 方法
- **Content-Type 在 converter 内部设置**，通过 `exchange.getIn().setHeader("Content-Type", "application/xml; charset=UTF-8")`，保持 Service 接口签名干净
- 参考项目中 `AriesKd100SignConverter`、`YcdSignConverter` 的注解模式

### 在 @Integrate 注解中引用

**仅 XML 响应（请求仍为 JSON）：**
```java
@Integrate.ResponseProcessor(
    serializableFun = XxxXmlSerializable.FUNCTION_NAME,
    serializableNamespace = EipConfigurationConstant.FUNCTION_NAMESPACE
)
```

**仅 XML 请求（响应为 JSON）：**
```java
@Integrate.RequestProcessor(
    inOutConverterFun = XxxXmlInOutConverter.FUNCTION_NAME,
    inOutConverterNamespace = EipConfigurationConstant.FUNCTION_NAMESPACE
)
```

**双向 XML（请求和响应都是 XML）：**
```java
@Integrate.RequestProcessor(
    finalResultKey = "request",
    inOutConverterFun = XxxXmlInOutConverter.FUNCTION_NAME,
    inOutConverterNamespace = EipConfigurationConstant.FUNCTION_NAMESPACE
)
@Integrate.ResponseProcessor(
    serializableFun = XxxXmlSerializable.FUNCTION_NAME,
    serializableNamespace = EipConfigurationConstant.FUNCTION_NAMESPACE
)
```

### XML 解析注意事项

- **编码**：确保 `DocumentBuilderFactory` 正确处理 UTF-8，XML 声明中的 `encoding` 属性会自动生效
- **命名空间**：如果 XML 含命名空间前缀（如 `<ns:OrderId>`），需在解析时剥离前缀或使用 namespace-aware 模式
- **CDATA**：`getTextContent()` 会自动处理 CDATA 区段，无需额外处理
- **特殊字符转义**：构建 XML 请求体时，必须对 `&`、`<`、`>`、`"`、`'` 进行转义
- **空节点**：解析前应检查 `NodeList.getLength() > 0`，避免 NPE
- **复杂 XML 建议**：如果 XML 结构超过 3 层嵌套或有多个命名空间，建议不使用 @Integrate，改用 JAXB + 手动 HTTP 的方式（参考项目中 `CxmlParserService`）

---

## TransientModel 规范

用于定义结构化的请求/响应体：

```java
import pro.shushi.pamirs.meta.annotation.Field;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.base.TransientModel;

@Model.model(XxxEipTransient.MODEL_MODEL)
@Model(displayName = "XXX传输模型")
public class XxxEipTransient extends TransientModel {
    public static final String MODEL_MODEL = "aries.eip.XxxEipTransient";

    @Field.String
    @Field(displayName = "字段说明")
    private String fieldName;

    // 不写 getter/setter，框架自动提供
}
```

---

## 关键 import 清单

```java
// 注解
import pro.shushi.pamirs.eip.api.annotation.Integrate;
import pro.shushi.pamirs.eip.api.annotation.IEipAnnotationSingletonConfig;
import pro.shushi.pamirs.meta.annotation.Field;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

// 枚举
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;

// 基类
import pro.shushi.pamirs.meta.base.IdModel;
import pro.shushi.pamirs.meta.base.TransientModel;

// EIP 核心类型
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.IEipAuthenticationProcessor;
import pro.shushi.pamirs.core.common.SuperMap;

// Spring
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;

// XML 格式扩展（按需引入）
import pro.shushi.pamirs.eip.api.IEipDeserialization;
import pro.shushi.pamirs.eip.api.IEipSerializable;
import pro.shushi.pamirs.eip.api.serializable.AbstractSerializable;
import pro.shushi.pamirs.eip.api.converter.DefaultInOutConverter;
import pro.shushi.pamirs.eip.api.constant.EipConfigurationConstant;

// XML DOM 解析（按需引入）
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

// Camel（InOutConverter 需要）
import org.apache.camel.ExtendedExchange;
```