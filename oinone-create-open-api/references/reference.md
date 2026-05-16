# 开放接口 @Open 完整注解参数与代码模板

## @Open 注解定义

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Open {
    String name() default "";        // 接口显示名称（默认取函数 displayName）
    Class<?> config() default Void.class;  // 配置类（开放接口中通常不需要）
    String path() default "";        // 请求路径（默认取方法名）

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Advanced {
        String httpMethod() default "";              // HTTP 方法（默认 POST）
        String inOutConverterFun() default "";       // 输入输出转换器函数
        String inOutConverterNamespace() default ""; // 转换器命名空间
        String authenticationProcessorFun() default "";       // 认证处理器函数
        String authenticationProcessorNamespace() default ""; // 认证处理器命名空间
        String serializableFun() default "";          // 序列化函数
        String serializableNamespace() default "";    // 序列化命名空间
        String deserializationFun() default "";       // 反序列化函数
        String deserializationNamespace() default ""; // 反序列化命名空间
    }
}
```

## 五种内置认证策略

所有策略的命名空间均为 `EipFunctionConstant.FUNCTION_NAMESPACE`（值为 `"pamirs.eip.default.namespace"`）。

| 常量名 | 说明 |
|--------|------|
| `DEFAULT_TOKEN_AUTHENTICATION_PROCESSOR_FUN` | 标准 Token 验证，当前项目默认使用 |
| `DEFAULT_NO_ENCRYPT_AUTHENTICATION_PROCESSOR_FUN` | 无加密 Token 验证（验证 accessToken 但不加密传输） |
| `DEFAULT_AUTHENTICATION_PROCESSOR_FUN` | 加密/解密认证（按 EipApplication 配置决定加密方式） |
| `DEFAULT_MD5_SIGNATURE_AUTHENTICATION_PROCESSOR_FUN` | MD5 签名校验 |
| `DEFAULT_APPLICATION_AUTHENTICATION_PROCESSOR_FUN` | 应用级认证 |

## OpenEipResult 返回类型

框架提供的标准响应包装类，所有开放接口方法必须返回此类型：

```java
// 包路径：pro.shushi.pamirs.eip.api.entity.openapi.OpenEipResult
public class OpenEipResult<T> {
    private Boolean success;     // 是否成功
    private String errorCode;    // 错误码（成功为 "0"）
    private String errorMsg;     // 错误信息（成功为 "ok"）
    private T data;              // 业务数据

    // 构造方法
    public OpenEipResult(T data);                              // 成功
    public OpenEipResult(String errorCode, String errorMsg);   // 失败

    // 静态工厂
    public static <T> OpenEipResult<T> success(T result);
    public static <T> OpenEipResult<T> error(String errorCode, String errorMsg);
}
```

响应 JSON 格式：
```json
{
  "success": true,
  "errorCode": "0",
  "errorMsg": "ok",
  "data": { "resultCode": "S", "resultMsg": "..." }
}
```

## IEipContext 上下文接口

开放接口方法的第一个参数，携带请求数据：

```java
// 包路径：pro.shushi.pamirs.eip.api.IEipContext
public interface IEipContext<T> {
    T getInterfaceContext();                              // 获取请求体数据（核心方法）
    Object getInterfaceContextValue(String key);          // 按 key 获取请求体字段
    void putInterfaceContextValue(String key, Object value);

    // 预定义上下文 key
    static final String HEADER_PARAMS_KEY = "http.headers";         // HTTP 请求头
    static final String URL_QUERY_PARAMS_KEY = "http.url.query.params"; // URL 查询参数
}
```

## 核心 import 列表

生成代码时必须包含以下 import：

```java
// 开放接口核心
import org.apache.camel.ExtendedExchange;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.annotation.Open;
import pro.shushi.pamirs.eip.api.constant.EipFunctionConstant;
import pro.shushi.pamirs.eip.api.entity.openapi.OpenEipResult;

// Pamirs 框架
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.util.JsonUtils;

// Spring
import org.springframework.stereotype.Service;
```

---

## 代码模板

### 模板 1：接口定义（Interface）

接口极简，仅声明 `FUN_NAMESPACE` 常量。放在 `*-eip-open-api` 模块。

```java
package pro.shushi.deli.aries.eip.open.api.api;

import pro.shushi.pamirs.meta.annotation.Fun;

/**
 * {接口用途描述}开放接口
 */
@Fun({InterfaceName}.FUN_NAMESPACE)
public interface {InterfaceName} {
    String FUN_NAMESPACE = "deli.aries.eip.{InterfaceName}";
}
```

**占位符**：
- `{InterfaceName}`：如 `DeliAriesOrderStatusEipService`

### 模板 2：实现类（Impl）— 使用内置认证

放在 `*-eip-core` 模块。这是核心文件，包含 `@Open` 注解。

```java
package pro.shushi.deli.aries.eip.core.service;

import org.apache.camel.ExtendedExchange;
import org.springframework.stereotype.Service;
import pro.shushi.deli.aries.common.constant.DeliAriesOpenApiConstants;
import pro.shushi.deli.aries.eip.open.api.api.{InterfaceName};
import pro.shushi.deli.aries.eip.open.api.tmodel.{ResponseDTO};
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.annotation.Open;
import pro.shushi.pamirs.eip.api.constant.EipFunctionConstant;
import pro.shushi.pamirs.eip.api.entity.openapi.OpenEipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.util.JsonUtils;

/**
 * {接口用途描述}开放接口实现
 */
@Slf4j
@Service
@Fun({InterfaceName}.FUN_NAMESPACE)
public class {ImplName} implements {InterfaceName} {

    @Function
    @Open(path = DeliAriesOpenApiConstants.{PATH_CONSTANT})
    @Open.Advanced(
            httpMethod = "{httpMethod}",
            authenticationProcessorFun = EipFunctionConstant.{AUTH_STRATEGY},
            authenticationProcessorNamespace = EipFunctionConstant.FUNCTION_NAMESPACE
    )
    public OpenEipResult<{ResponseDTO}> {methodName}(IEipContext<SuperMap> context, ExtendedExchange exchange) {
        {ResponseDTO} response = new {ResponseDTO}();
        response.setResultCode("S");
        try {
            // TODO: 解析请求参数
            // 方式一：整体反序列化
            // XxxRequest request = JsonUtils.parseObject(
            //     JsonUtils.toJSONString(context.getInterfaceContext()), XxxRequest.class);
            //
            // 方式二：按字段读取
            // String fieldValue = String.valueOf(context.getInterfaceContext().getIteration("fieldName"));

            // TODO: 实现业务逻辑

            response.setResultMsg("success");
        } catch (Exception e) {
            log.error("{methodName} error", e);
            response.setResultCode("E");
            response.setResultMsg(e.getMessage());
        }
        return new OpenEipResult<>(response);
    }
}
```

**占位符**：
- `{InterfaceName}`：接口类名
- `{ImplName}`：实现类名（接口名 + `Impl`）
- `{ResponseDTO}`：响应 DTO 类名
- `{PATH_CONSTANT}`：常量类中的 path 常量名
- `{httpMethod}`：`post` 或 `get`
- `{AUTH_STRATEGY}`：认证策略常量名
- `{methodName}`：方法名（小驼峰）

### 模板 3：Response DTO

放在 `*-eip-open-api` 模块。使用 Pamirs 的 `@Data`（**不是 Lombok**）。

```java
package pro.shushi.deli.aries.eip.open.api.tmodel;

import pro.shushi.pamirs.meta.annotation.fun.Data;

/**
 * {接口用途描述}开放接口响应
 */
@Data
public class {ResponseDTO} {

    private String resultCode; // 返回状态 S:成功 E:失败
    private String resultMsg;  // 返回信息
    // TODO: 根据业务需要添加返回字段
}
```

**关键注意**：`@Data` 来自 `pro.shushi.pamirs.meta.annotation.fun.Data`，不是 Lombok。这个注解会自动生成 getter/setter。

### 模板 4：自定义认证处理器（可选）

仅在用户选择「自定义认证」时生成。

```java
package pro.shushi.deli.aries.eip.core.service;

import org.apache.camel.ExtendedExchange;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipAuthenticationProcessor;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.constant.EipFunctionConstant;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

/**
 * {接口用途描述}开放接口认证处理器
 */
@Component
@Fun({AuthProcessorName}.FUN_NAMESPACE)
public class {AuthProcessorName} implements IEipAuthenticationProcessor<SuperMap> {
    public static final String FUN_NAMESPACE = "eip.{bizDomain}.{AuthProcessorName}";
    public static final String FUN = "{methodName}";

    @Function.fun(FUN)
    @Function.Advanced(displayName = "开放接口Token有效性认证")
    @Function(name = EipFunctionConstant.DEFAULT_TOKEN_AUTHENTICATION_PROCESSOR_FUN)
    public Boolean {methodName}(IEipContext<SuperMap> context, ExtendedExchange exchange) {
        return authentication(context, exchange);
    }

    @Override
    public boolean authentication(IEipContext<SuperMap> context, ExtendedExchange exchange) {
        // TODO: 实现认证逻辑
        // 返回 true 表示认证通过，false 表示认证失败
        return true;
    }
}
```

使用自定义认证时，实现类的 `@Open.Advanced` 需引用此处理器：
```java
@Open.Advanced(
    httpMethod = "post",
    authenticationProcessorFun = {AuthProcessorName}.FUN,
    authenticationProcessorNamespace = {AuthProcessorName}.FUN_NAMESPACE
)
```

### 模板 5：EIP 初始化类（BizInit）

通常已存在，仅在目标模块没有时生成。

```java
package pro.shushi.deli.aries.eip.core.init;

import org.springframework.stereotype.Component;
import pro.shushi.deli.aries.eip.api.DeliAriesEipModule;
import pro.shushi.pamirs.boot.common.api.command.AppLifecycleCommand;
import pro.shushi.pamirs.boot.common.api.init.InstallDataInit;
import pro.shushi.pamirs.boot.common.api.init.ReloadDataInit;
import pro.shushi.pamirs.boot.common.api.init.UpgradeDataInit;
import pro.shushi.pamirs.eip.api.annotation.EipResolver;

import java.util.Collections;
import java.util.List;

@Component
public class {BizInitName} implements InstallDataInit, UpgradeDataInit, ReloadDataInit {

    @Override
    public boolean init(AppLifecycleCommand command, String version) {
        initEip();
        return Boolean.TRUE;
    }

    @Override
    public boolean reload(AppLifecycleCommand command, String version) {
        initEip();
        return Boolean.TRUE;
    }

    @Override
    public boolean upgrade(AppLifecycleCommand command, String version, String existVersion) {
        initEip();
        return Boolean.TRUE;
    }

    @Override
    public List<String> modules() {
        return Collections.singletonList({ModuleClass}.MODULE_MODULE);
    }

    @Override
    public int priority() {
        return 0;
    }

    private void initEip() {
        EipResolver.resolver({ModuleClass}.MODULE_MODULE, null);
    }
}
```

## 请求数据解析模式

开放接口的请求体通过 `context.getInterfaceContext()` 获取，返回 `SuperMap`（扩展 HashMap）。两种解析方式：

### 方式一：整体反序列化（推荐）

```java
XxxRequest request = JsonUtils.parseObject(
    JsonUtils.toJSONString(context.getInterfaceContext()), XxxRequest.class);
```

### 方式二：按字段读取

```java
Map<String, Object> requestMap = JsonUtils.parseMap(
    JsonUtils.toJSONString(context.getInterfaceContext()));
String value = String.valueOf(requestMap.get("fieldName"));
```

### 方式三：通过 SuperMap 迭代器

```java
String id = Optional.ofNullable(
    String.valueOf(context.getInterfaceContext().getIteration("id")))
    .orElse("");
```

## deli-b2b-oversea 文件位置速查

| 文件类型 | 目录 |
|---------|------|
| 接口 | `deli-b2b-oversea/deli-aries-eip/deli-aries-eip-open-api/src/main/java/pro/shushi/deli/aries/eip/open/api/api/` |
| 实现类 | `deli-b2b-oversea/deli-aries-eip/deli-aries-eip-core/src/main/java/pro/shushi/deli/aries/eip/core/service/` |
| Response DTO | `deli-b2b-oversea/deli-aries-eip/deli-aries-eip-open-api/src/main/java/pro/shushi/deli/aries/eip/open/api/tmodel/` |
| Path 常量 | `deli-b2b-oversea/deli-aries-common/src/main/java/pro/shushi/deli/aries/common/constant/DeliAriesOpenApiConstants.java` |
| BizInit | `deli-b2b-oversea/deli-aries-eip/deli-aries-eip-core/src/main/java/pro/shushi/deli/aries/eip/core/thirid/init/DeliAriesEipBizInit.java`（已存在） |
| 认证处理器 | `deli-b2b-oversea/deli-aries-eip/deli-aries-eip-core/src/main/java/pro/shushi/deli/aries/eip/core/service/`（按业务分子包） |