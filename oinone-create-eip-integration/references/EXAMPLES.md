# EIP 集成接口 — 代码模板与示例

基于当前项目 deli-m4 中的 4 个真实 `@Integrate` 实现，提炼出以下模板。

---

## 模板 1：最简模式（基于 ZORO 订单确认）

适用场景：简单的 POST 请求，无需认证，无需参数映射。

### 1.1 Service 接口

```java
package pro.shushi.deli.aries.eip.api.api.{domain};

import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Fun(DeliAries{Vendor}EipDataService.FUN_NAMESPACE)
public interface DeliAries{Vendor}EipDataService {
    String FUN_NAMESPACE = "aries.eip.DeliAries{Vendor}EipDataService";

    @Function
    EipResult<SuperMap> push{Action}({RequestType} request);
}
```

### 1.2 EipConfig 模型

```java
package pro.shushi.deli.aries.eip.api.model;

import pro.shushi.pamirs.eip.api.annotation.IEipAnnotationSingletonConfig;
import pro.shushi.pamirs.meta.annotation.Field;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.base.IdModel;
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;

@Model.model(DeliAries{Vendor}EipConfig.MODEL_MODEL)
@Model(displayName = "{DisplayName}接口配置")
public class DeliAries{Vendor}EipConfig extends IdModel
        implements IEipAnnotationSingletonConfig<DeliAries{Vendor}EipConfig> {

    public static final String MODEL_MODEL = "aries.eip.DeliAries{Vendor}EipConfig";

    @Field(displayName = "协议", defaultValue = "https")
    private String schema;

    @Field(displayName = "服务端地址")
    private String host;

    @Function(openLevel = FunctionOpenEnum.API, summary = "{DisplayName}接口配置构造")
    @Function.Advanced(type = FunctionTypeEnum.QUERY)
    public DeliAries{Vendor}EipConfig construct(DeliAries{Vendor}EipConfig config) {
        DeliAries{Vendor}EipConfig eipConfig =
                (config == null ? new DeliAries{Vendor}EipConfig() : config).singletonModel();
        if (eipConfig != null) {
            return eipConfig;
        }
        return null;
    }
}
```

### 1.3 Constants 常量

```java
package pro.shushi.deli.aries.eip.api.conf;

public class DeliAries{Vendor}Constants {

    // {Action}接口路径
    public static final String {ACTION_PATH} = "/api/v1/{endpoint}";
}
```

### 1.4 Service 实现类

```java
package pro.shushi.deli.aries.eip.core.service.{domain};

import org.springframework.stereotype.Service;
import pro.shushi.deli.aries.eip.api.api.{domain}.DeliAries{Vendor}EipDataService;
import pro.shushi.deli.aries.eip.api.conf.DeliAries{Vendor}Constants;
import pro.shushi.deli.aries.eip.api.model.DeliAries{Vendor}EipConfig;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.annotation.Integrate;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

@Slf4j
@Service
@Fun(DeliAries{Vendor}EipDataService.FUN_NAMESPACE)
public class DeliAries{Vendor}EipDataServiceImpl implements DeliAries{Vendor}EipDataService {

    @Integrate(config = DeliAries{Vendor}EipConfig.class)
    @Integrate.Advanced(path = DeliAries{Vendor}Constants.{ACTION_PATH})
    @Function
    @Override
    public EipResult<SuperMap> push{Action}({RequestType} request) {
        return null;
    }
}
```

---

## 模板 2：带 Header 参数映射（基于 OutOrder SAP 对接）

适用场景：需要先获取 Token，后续请求将 Token 放入 HTTP Header。

### 2.1 Service 接口

```java
package pro.shushi.deli.aries.eip.api.api.{domain};

import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Fun(DeliAries{Vendor}EipDataService.FUN_NAMESPACE)
public interface DeliAries{Vendor}EipDataService {
    String FUN_NAMESPACE = "aries.eip.DeliAries{Vendor}EipDataService";

    @Function
    EipResult<SuperMap> queryToken(String username, String password);

    @Function
    EipResult<SuperMap> push{Action}({RequestType} request, String token);
}
```

### 2.2 EipConfig 模型（带认证字段）

```java
package pro.shushi.deli.aries.eip.api.model;

import pro.shushi.pamirs.eip.api.annotation.IEipAnnotationSingletonConfig;
import pro.shushi.pamirs.meta.annotation.Field;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.base.IdModel;
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;

@Model.model(DeliAries{Vendor}EipConfig.MODEL_MODEL)
@Model(displayName = "{DisplayName}集成配置")
public class DeliAries{Vendor}EipConfig extends IdModel
        implements IEipAnnotationSingletonConfig<DeliAries{Vendor}EipConfig> {

    public static final String MODEL_MODEL = "aries.eip.DeliAries{Vendor}EipConfig";

    @Field(displayName = "服务端域名")
    private String host;

    @Field(displayName = "schema", defaultValue = "https")
    private String schema;

    @Field(displayName = "用户名")
    private String username;

    @Field(displayName = "密码")
    private String password;

    @Function(openLevel = FunctionOpenEnum.API, summary = "{DisplayName}配置构造")
    @Function.Advanced(type = FunctionTypeEnum.QUERY)
    public DeliAries{Vendor}EipConfig construct(DeliAries{Vendor}EipConfig config) {
        DeliAries{Vendor}EipConfig eipConfig = config.singletonModel();
        if (eipConfig != null) {
            return eipConfig;
        }
        return null;
    }
}
```

### 2.3 Constants

```java
package pro.shushi.deli.aries.eip.api.conf;

public class DeliAries{Vendor}Constants {

    // 获取 token
    public static final String QUERY_TOKEN = "/api/login";
    // 推送数据
    public static final String PUSH_{ACTION} = "/api/v1/{endpoint}";
}
```

### 2.4 Service 实现类（带 token→header 映射）

```java
package pro.shushi.deli.aries.eip.core.service.{domain};

import org.springframework.stereotype.Service;
import pro.shushi.deli.aries.eip.api.api.{domain}.DeliAries{Vendor}EipDataService;
import pro.shushi.deli.aries.eip.api.conf.DeliAries{Vendor}Constants;
import pro.shushi.deli.aries.eip.api.model.DeliAries{Vendor}EipConfig;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.annotation.Integrate;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

@Slf4j
@Service
@Fun(DeliAries{Vendor}EipDataService.FUN_NAMESPACE)
public class DeliAries{Vendor}EipDataServiceImpl implements DeliAries{Vendor}EipDataService {

    @Integrate(config = DeliAries{Vendor}EipConfig.class)
    @Integrate.Advanced(path = DeliAries{Vendor}Constants.QUERY_TOKEN)
    @Function
    @Override
    public EipResult<SuperMap> queryToken(String username, String password) {
        return null;
    }

    @Integrate(config = DeliAries{Vendor}EipConfig.class)
    @Integrate.Advanced(path = DeliAries{Vendor}Constants.PUSH_{ACTION})
    @Integrate.RequestProcessor(
            finalResultKey = "request",
            convertParams = {
                    @Integrate.ConvertParam(
                            inParam = "token",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".token")
            }
    )
    @Function
    @Override
    public EipResult<SuperMap> push{Action}({RequestType} request, String token) {
        return null;
    }
}
```

---

## 模板 3：多端点 + 多 Header 参数（基于 3PL 物流对接）

适用场景：一个第三方系统有多个 API 端点，每个请求都需要传递签名等 header 参数。

### 3.1 Service 接口（多方法）

```java
package pro.shushi.deli.aries.eip.api.api.{domain};

import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Fun(DeliAries{Vendor}EipDataService.FUN_NAMESPACE)
public interface DeliAries{Vendor}EipDataService {
    String FUN_NAMESPACE = "aries.eip.DeliAries{Vendor}EipDataService";

    @Function
    EipResult<SuperMap> push{Action1}(SuperMap request, String authorization,
                                      String timestamp, String appKey, String sign);

    @Function
    EipResult<SuperMap> push{Action2}(SuperMap request, String authorization,
                                      String timestamp, String appKey, String sign);

    @Function
    EipResult<SuperMap> query{Action3}(SuperMap request, String authorization,
                                       String timestamp, String appKey, String sign);
}
```

### 3.2 EipConfig（带 AppKey/Secret/Token）

```java
package pro.shushi.deli.aries.eip.api.model;

import pro.shushi.pamirs.eip.api.annotation.IEipAnnotationSingletonConfig;
import pro.shushi.pamirs.meta.annotation.Field;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.base.IdModel;
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;

@Model.model(DeliAries{Vendor}EipConfig.MODEL_MODEL)
@Model(displayName = "{DisplayName}配置信息")
public class DeliAries{Vendor}EipConfig extends IdModel
        implements IEipAnnotationSingletonConfig<DeliAries{Vendor}EipConfig> {

    public static final String MODEL_MODEL = "aries.eip.DeliAries{Vendor}EipConfig";

    @Field(displayName = "服务端域名")
    private String host;

    @Field(displayName = "schema", defaultValue = "https")
    private String schema;

    @Field(displayName = "AppKey")
    private String appKey;

    @Field(displayName = "AppSecret")
    private String appSecret;

    @Field(displayName = "token")
    private String token;

    @Function(openLevel = FunctionOpenEnum.API, summary = "{DisplayName}配置构造")
    @Function.Advanced(type = FunctionTypeEnum.QUERY)
    public DeliAries{Vendor}EipConfig construct(DeliAries{Vendor}EipConfig config) {
        DeliAries{Vendor}EipConfig eipConfig = config.singletonModel();
        if (eipConfig != null) {
            return eipConfig;
        }
        return null;
    }
}
```

### 3.3 Constants（多路径）

```java
package pro.shushi.deli.aries.eip.api.conf;

public class DeliAries{Vendor}Constants {

    public static final String {ACTION1_PATH} = "/webApi/v1/{endpoint1}";
    public static final String {ACTION2_PATH} = "/webApi/v1/{endpoint2}";
    public static final String {ACTION3_PATH} = "/webApi/v1/{endpoint3}";
}
```

### 3.4 Service 实现类（多方法 + 统一的 header 映射）

```java
package pro.shushi.deli.aries.eip.core.service.{domain};

import org.springframework.stereotype.Service;
import pro.shushi.deli.aries.eip.api.api.{domain}.DeliAries{Vendor}EipDataService;
import pro.shushi.deli.aries.eip.api.conf.DeliAries{Vendor}Constants;
import pro.shushi.deli.aries.eip.api.model.DeliAries{Vendor}EipConfig;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.annotation.Integrate;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

@Slf4j
@Service
@Fun(DeliAries{Vendor}EipDataService.FUN_NAMESPACE)
public class DeliAries{Vendor}EipDataServiceImpl implements DeliAries{Vendor}EipDataService {

    @Integrate(config = DeliAries{Vendor}EipConfig.class)
    @Integrate.Advanced(path = DeliAries{Vendor}Constants.{ACTION1_PATH})
    @Integrate.RequestProcessor(
            finalResultKey = "request",
            convertParams = {
                    @Integrate.ConvertParam(inParam = "timestamp",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".timestamp"),
                    @Integrate.ConvertParam(inParam = "appKey",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".appKey"),
                    @Integrate.ConvertParam(inParam = "sign",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".sign"),
                    @Integrate.ConvertParam(inParam = "authorization",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".Authorization")
            }
    )
    @Function
    @Override
    public EipResult<SuperMap> push{Action1}(SuperMap request, String authorization,
                                              String timestamp, String appKey, String sign) {
        return null;
    }

    // 其他方法结构完全相同，仅 path 常量和方法名不同
    @Integrate(config = DeliAries{Vendor}EipConfig.class)
    @Integrate.Advanced(path = DeliAries{Vendor}Constants.{ACTION2_PATH})
    @Integrate.RequestProcessor(
            finalResultKey = "request",
            convertParams = {
                    @Integrate.ConvertParam(inParam = "timestamp",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".timestamp"),
                    @Integrate.ConvertParam(inParam = "appKey",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".appKey"),
                    @Integrate.ConvertParam(inParam = "sign",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".sign"),
                    @Integrate.ConvertParam(inParam = "authorization",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".Authorization")
            }
    )
    @Function
    @Override
    public EipResult<SuperMap> push{Action2}(SuperMap request, String authorization,
                                              String timestamp, String appKey, String sign) {
        return null;
    }
}
```

---

## 模板 4：完整模式 — Auth 处理器 + Exception 处理器 + Response 映射（基于 Caelum）

适用场景：需要动态获取认证 token、自定义异常判定、响应字段映射。

### 4.1 Service 接口

```java
package pro.shushi.deli.aries.eip.api.api;

import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Fun(DeliAries{Vendor}EipService.FUN_NAME)
public interface DeliAries{Vendor}EipService {
    String FUN_NAME = "deli.aries.eip.DeliAries{Vendor}EipService";

    @Function
    EipResult<SuperMap> query{Resource}();

    @Function
    EipResult<SuperMap> fetchAccessToken(String appkey, String appSecret);
}
```

### 4.2 EipConfig（带 AppKey/Secret）

```java
package pro.shushi.deli.aries.eip.api.conf;

import pro.shushi.pamirs.eip.api.annotation.IEipAnnotationSingletonConfig;
import pro.shushi.pamirs.meta.annotation.Field;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.base.IdModel;
import pro.shushi.pamirs.meta.enmu.FunctionOpenEnum;
import pro.shushi.pamirs.meta.enmu.FunctionTypeEnum;

@Model.model(DeliAries{Vendor}Config.MODEL_MODEL)
@Model.Advanced(unique = {"code"})
@Model(displayName = "{DisplayName}接口配置模型")
public class DeliAries{Vendor}Config extends IdModel
        implements IEipAnnotationSingletonConfig<DeliAries{Vendor}Config> {

    public static final String MODEL_MODEL = "deli.aries.eip.DeliAries{Vendor}Config";

    @Field.String
    @Field(displayName = "服务端域名")
    private String host;

    @Field.String
    @Field(displayName = "请求协议Http或Https")
    private String schema;

    @Field.String
    @Field(displayName = "appKey")
    private String appKey;

    @Field.String(size = 4096)
    @Field(displayName = "appSecret")
    private String appSecret;

    @Field.String
    @Field(displayName = "code")
    private String code;

    @Function(openLevel = FunctionOpenEnum.API, summary = "{DisplayName}配置构造")
    @Function.Advanced(type = FunctionTypeEnum.QUERY)
    public DeliAries{Vendor}Config construct(DeliAries{Vendor}Config config) {
        DeliAries{Vendor}Config config1 = config.singletonModel();
        if (config1 != null) {
            return config1;
        }
        return config.construct();
    }
}
```

### 4.3 Auth 处理器

```java
package pro.shushi.deli.aries.eip.core.thirid.{domain};

import org.apache.camel.ExtendedExchange;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.deli.aries.eip.api.api.DeliAries{Vendor}EipService;
import pro.shushi.deli.aries.eip.api.conf.DeliAries{Vendor}Config;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipAuthenticationProcessor;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Fun(DeliAries{Vendor}AuthFunction.FUN_NAMESPACE)
@Component
public class DeliAries{Vendor}AuthFunction implements IEipAuthenticationProcessor<SuperMap> {

    public static final String FUN_NAMESPACE = "deli.aries.eip.DeliAries{Vendor}AuthFunction";
    public static final String FUN = "{vendor}Authentication";

    @Autowired
    private DeliAries{Vendor}EipService eipService;

    @Function
    @Function.fun(FUN)
    public Boolean {vendor}Authentication(IEipContext<SuperMap> context, ExtendedExchange exchange) {
        return authentication(context, exchange);
    }

    @Override
    public boolean authentication(IEipContext<SuperMap> context, ExtendedExchange exchange) {
        // 获取配置
        DeliAries{Vendor}Config config = new DeliAries{Vendor}Config().singletonModel();
        // TODO: 实现 token 获取逻辑（缓存 + 刷新）
        // 获取 accessToken
        EipResult<SuperMap> eipResult = eipService.fetchAccessToken(
                config.getAppKey(), config.getAppSecret());
        if (eipResult.getSuccess()) {
            String accessToken = String.valueOf(
                    eipResult.getContext().getInterfaceContextValue("accessToken"));
            if (StringUtils.isBlank(accessToken)) {
                return Boolean.FALSE;
            }
            context.putInterfaceContextValue(
                    IEipContext.HEADER_PARAMS_KEY + ".accessToken", accessToken);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
```

### 4.4 Exception 判定函数

```java
package pro.shushi.deli.aries.eip.core.thirid.{domain};

import pro.shushi.pamirs.core.common.StringHelper;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

import static pro.shushi.pamirs.eip.api.IEipContext.DEFAULT_ERROR_CODE_KEY;

@Fun(DeliAries{Vendor}ExceptionPredictFunction.FUN_NAMESPACE)
public class DeliAries{Vendor}ExceptionPredictFunction {

    public static final String FUN_NAMESPACE = "deli.aries.eip.DeliAries{Vendor}ExceptionPredictFunction";
    public static final String FUN = "{vendor}ExpFunction";

    @Function
    @Function.fun(FUN)
    public Boolean {vendor}ExpFunction(IEipContext<SuperMap> context) {
        // 返回 true 表示判定为异常
        // 根据第三方 API 的返回格式自定义判断逻辑
        return !Boolean.TRUE.toString().equals(
                StringHelper.valueOf(context.getExecutorContextValue(DEFAULT_ERROR_CODE_KEY)));
    }
}
```

### 4.5 Service 实现类（完整版）

```java
package pro.shushi.deli.aries.eip.core.thirid.{domain};

import org.springframework.stereotype.Component;
import pro.shushi.deli.aries.eip.api.api.DeliAries{Vendor}EipService;
import pro.shushi.deli.aries.eip.api.conf.DeliAries{Vendor}Config;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.annotation.Integrate;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

@Slf4j
@Fun(DeliAries{Vendor}EipService.FUN_NAME)
@Component
public class DeliAries{Vendor}EipServiceImpl implements DeliAries{Vendor}EipService {

    @Function
    @Override
    @Integrate(config = DeliAries{Vendor}Config.class)
    @Integrate.Advanced(path = "/api/v1/{resource}")
    @Integrate.RequestProcessor(
            authenticationProcessorNamespace = DeliAries{Vendor}AuthFunction.FUN_NAMESPACE,
            authenticationProcessorFun = DeliAries{Vendor}AuthFunction.FUN
    )
    @Integrate.ExceptionProcessor(
            errorCode = "success",
            exceptionPredictFun = DeliAries{Vendor}ExceptionPredictFunction.FUN,
            exceptionPredictNamespace = DeliAries{Vendor}ExceptionPredictFunction.FUN_NAMESPACE
    )
    public EipResult<SuperMap> query{Resource}() {
        return null;
    }

    @Function
    @Override
    @Integrate(config = DeliAries{Vendor}Config.class)
    @Integrate.Advanced(path = "/api/v1/access-token")
    @Integrate.RequestProcessor(
            convertParams = {
                    @Integrate.ConvertParam(
                            inParam = "appkey",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".appkey"),
                    @Integrate.ConvertParam(
                            inParam = "appSecret",
                            outParam = IEipContext.HEADER_PARAMS_KEY + ".appSecret")
            }
    )
    @Integrate.ResponseProcessor(
            convertParams = {
                    @Integrate.ConvertParam(
                            inParam = "access_token",
                            outParam = "accessToken"),
            }
    )
    public EipResult<SuperMap> fetchAccessToken(String appkey, String appSecret) {
        return null;
    }
}
```

---

## 模板 5：Transient 请求体模型

当请求体有固定的结构化字段时，用 TransientModel 替代 SuperMap。

```java
package pro.shushi.deli.aries.eip.api.tmodel.{domain};

import pro.shushi.pamirs.meta.annotation.Field;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.base.TransientModel;

import java.util.List;

@Model.model(DeliAries{Vendor}{Action}EipTransient.MODEL_MODEL)
@Model(displayName = "{DisplayName}传输模型")
public class DeliAries{Vendor}{Action}EipTransient extends TransientModel {

    public static final String MODEL_MODEL = "deli.aries.eip.DeliAries{Vendor}{Action}EipTransient";

    @Field.String
    @Field(displayName = "订单编号")
    private String orderNo;

    @Field.String
    @Field(displayName = "时间")
    private String dateTime;

    @Field(displayName = "明细列表")
    private List<DeliAries{Vendor}{Action}DetailEipTransient> detailList;
}
```

嵌套的明细 Transient：

```java
package pro.shushi.deli.aries.eip.api.tmodel.{domain};

import pro.shushi.pamirs.meta.annotation.Field;
import pro.shushi.pamirs.meta.annotation.Model;
import pro.shushi.pamirs.meta.base.TransientModel;

@Model.model(DeliAries{Vendor}{Action}DetailEipTransient.MODEL_MODEL)
@Model(displayName = "{DisplayName}明细传输模型")
public class DeliAries{Vendor}{Action}DetailEipTransient extends TransientModel {

    public static final String MODEL_MODEL = "deli.aries.eip.DeliAries{Vendor}{Action}DetailEipTransient";

    @Field.String
    @Field(displayName = "商品编码")
    private String itemCode;

    @Field.Integer
    @Field(displayName = "数量")
    private Integer quantity;
}
```

---

## 模板 6：XML 响应解析 — 自定义 Serializable（@Integrate 模式）

适用场景：第三方接口返回 XML 格式响应，请求仍为 JSON（或无请求体）。需要自定义 Serializable 将 XML 解析为 SuperMap。

> **Service 接口、EipConfig、Constants 的结构与 JSON 模式（模板 1-4）完全相同**，此处不重复。唯一区别在于 Service 实现类和新增的 XML Serializable 类。

### 6.1 XML Serializable

```java
package pro.shushi.deli.aries.eip.core.thirid.{domain}.serializable;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

/**
 * {DisplayName} XML 响应反序列化
 * 将第三方返回的 XML 字符串解析为 SuperMap
 */
@Slf4j
@Component
@Fun(EipConfigurationConstant.FUNCTION_NAMESPACE)
public class DeliAries{Vendor}XmlSerializable extends AbstractSerializable
        implements IEipSerializable<SuperMap>, IEipDeserialization<SuperMap> {

    public static final String FUNCTION_NAME =
            EipConfigurationConstant.SERIALIZABLE_PREFIX
            + "DeliAries{Vendor}XmlSerializable" + "stringToSuperMap";

    @Override
    @Function.Advanced(displayName = "{DisplayName} XML反序列化")
    @Function.fun(FUNCTION_NAME)
    public SuperMap serializable(Object inObject) {
        return super.serializable(inObject);
    }

    /**
     * 将 XML 字符串解析为 SuperMap
     *
     * 示例 XML 响应：
     * <Response>
     *   <Code>200</Code>
     *   <Message>OK</Message>
     *   <Data>
     *     <OrderId>ORD-12345</OrderId>
     *     <Status>CONFIRMED</Status>
     *   </Data>
     * </Response>
     */
    @Override
    protected SuperMap stringToSuperMap(String s) {
        SuperMap result = new SuperMap();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(s)));
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();

            // 提取一级字段
            result.put("code", getElementText(root, "Code"));
            result.put("message", getElementText(root, "Message"));

            // 提取嵌套节点中的字段
            NodeList dataNodes = root.getElementsByTagName("Data");
            if (dataNodes.getLength() > 0) {
                Element dataElement = (Element) dataNodes.item(0);
                result.put("orderId", getElementText(dataElement, "OrderId"));
                result.put("status", getElementText(dataElement, "Status"));
            }
        } catch (Exception e) {
            log.error("{DisplayName} XML 解析失败: {}", s, e);
            // 解析失败时将原始字符串放入 result，方便排查
            result.put(EipConfigurationConstant.DEFAULT_RESULT_KEY, s);
        }
        return result;
    }

    /**
     * 安全地获取 XML 元素的文本内容
     */
    private String getElementText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return null;
    }
}
```

### 6.2 Service 实现类（引用 XML Serializable）

```java
package pro.shushi.deli.aries.eip.core.service.{domain};

import org.springframework.stereotype.Service;
import pro.shushi.deli.aries.eip.api.api.{domain}.DeliAries{Vendor}EipDataService;
import pro.shushi.deli.aries.eip.api.conf.DeliAries{Vendor}Constants;
import pro.shushi.deli.aries.eip.api.model.DeliAries{Vendor}EipConfig;
import pro.shushi.deli.aries.eip.core.thirid.{domain}.serializable.DeliAries{Vendor}XmlSerializable;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.annotation.Integrate;
import pro.shushi.pamirs.eip.api.constant.EipConfigurationConstant;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

@Slf4j
@Service
@Fun(DeliAries{Vendor}EipDataService.FUN_NAMESPACE)
public class DeliAries{Vendor}EipDataServiceImpl implements DeliAries{Vendor}EipDataService {

    @Integrate(config = DeliAries{Vendor}EipConfig.class)
    @Integrate.Advanced(path = DeliAries{Vendor}Constants.QUERY_{ACTION})
    @Integrate.ResponseProcessor(
            serializableFun = DeliAries{Vendor}XmlSerializable.FUNCTION_NAME,
            serializableNamespace = EipConfigurationConstant.FUNCTION_NAMESPACE
    )
    @Function
    @Override
    public EipResult<SuperMap> query{Action}(SuperMap request) {
        return null;
    }
}
```

### 6.3 产物清单与说明

| # | 文件 | 与 JSON 模板的差异 |
|---|------|-------------------|
| 1 | Service 接口 | 无差异 |
| 2 | EipConfig 模型 | 无差异 |
| 3 | Constants 常量 | 无差异 |
| 4 | Service 实现类 | **增加** `@Integrate.ResponseProcessor(serializableFun=..., serializableNamespace=...)` |
| 5 | **XML Serializable** | **新增文件**，放在 `eip-core` 的 `thirid/{domain}/serializable/` 下 |

---

## 模板 7：双向 XML — 自定义 Serializable + InOutConverter（@Integrate 模式）

适用场景：请求体和响应体都是 XML 格式。需要自定义 InOutConverter 将 SuperMap 转为 XML 发送，同时自定义 Serializable 解析 XML 响应。

> XML Serializable 复用模板 6 的实现。此处仅展示新增的 InOutConverter 和完整的 Service 实现类注解组合。

### 7.1 XML InOutConverter

```java
package pro.shushi.deli.aries.eip.core.thirid.{domain}.converter;

import org.apache.camel.ExtendedExchange;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.constant.EipConfigurationConstant;
import pro.shushi.pamirs.eip.api.converter.DefaultInOutConverter;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

/**
 * {DisplayName} XML 请求体转换器
 * 将 SuperMap 请求数据转换为 XML 字符串
 */
@Slf4j
@Component
@Fun(EipConfigurationConstant.FUNCTION_NAMESPACE)
public class DeliAries{Vendor}XmlInOutConverter extends DefaultInOutConverter {

    public static final String FUNCTION_NAME =
            EipConfigurationConstant.IN_OUT_CONVERTER_PREFIX
            + "DeliAries{Vendor}XmlInOutConverter_" + "exchangeObject";

    /**
     * 示例 XML 请求体：
     * <?xml version="1.0" encoding="UTF-8"?>
     * <Request>
     *   <OrderNo>ORD-12345</OrderNo>
     *   <Quantity>10</Quantity>
     * </Request>
     */
    @Override
    @Function.fun(FUNCTION_NAME)
    @Function.Advanced(displayName = "{DisplayName} XML请求体转换")
    public Object exchangeObject(ExtendedExchange exchange, Object inObject) throws Exception {
        // 设置 Content-Type（在 converter 中设置，不污染 Service 接口签名）
        exchange.getIn().setHeader("Content-Type", "application/xml; charset=UTF-8");

        SuperMap body = (SuperMap) inObject;
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<Request>");

        // 根据实际请求结构构建 XML（以下为示例，需按第三方接口文档调整）
        if (body.get("orderNo") != null) {
            xml.append("<OrderNo>").append(escapeXml(String.valueOf(body.get("orderNo")))).append("</OrderNo>");
        }
        if (body.get("quantity") != null) {
            xml.append("<Quantity>").append(body.get("quantity")).append("</Quantity>");
        }

        xml.append("</Request>");

        log.info("{DisplayName} 发送 XML 请求: {}", xml);
        return xml.toString();
    }

    /**
     * XML 特殊字符转义，防止注入和解析错误
     */
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;")
                     .replace("'", "&apos;");
    }
}
```

### 7.2 Service 实现类（双向 XML 完整版）

```java
package pro.shushi.deli.aries.eip.core.service.{domain};

import org.springframework.stereotype.Service;
import pro.shushi.deli.aries.eip.api.api.{domain}.DeliAries{Vendor}EipDataService;
import pro.shushi.deli.aries.eip.api.conf.DeliAries{Vendor}Constants;
import pro.shushi.deli.aries.eip.api.model.DeliAries{Vendor}EipConfig;
import pro.shushi.deli.aries.eip.core.thirid.{domain}.converter.DeliAries{Vendor}XmlInOutConverter;
import pro.shushi.deli.aries.eip.core.thirid.{domain}.serializable.DeliAries{Vendor}XmlSerializable;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.annotation.Integrate;
import pro.shushi.pamirs.eip.api.constant.EipConfigurationConstant;
import pro.shushi.pamirs.eip.api.entity.EipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;

@Slf4j
@Service
@Fun(DeliAries{Vendor}EipDataService.FUN_NAMESPACE)
public class DeliAries{Vendor}EipDataServiceImpl implements DeliAries{Vendor}EipDataService {

    @Integrate(config = DeliAries{Vendor}EipConfig.class)
    @Integrate.Advanced(path = DeliAries{Vendor}Constants.PUSH_{ACTION})
    @Integrate.RequestProcessor(
            finalResultKey = "request",
            inOutConverterFun = DeliAries{Vendor}XmlInOutConverter.FUNCTION_NAME,
            inOutConverterNamespace = EipConfigurationConstant.FUNCTION_NAMESPACE
    )
    @Integrate.ResponseProcessor(
            serializableFun = DeliAries{Vendor}XmlSerializable.FUNCTION_NAME,
            serializableNamespace = EipConfigurationConstant.FUNCTION_NAMESPACE
    )
    @Function
    @Override
    public EipResult<SuperMap> push{Action}(SuperMap request) {
        return null;
    }
}
```

### 7.3 产物清单与说明

| # | 文件 | 与 JSON 模板的差异 |
|---|------|-------------------|
| 1 | Service 接口 | 无差异 |
| 2 | EipConfig 模型 | 无差异 |
| 3 | Constants 常量 | 无差异 |
| 4 | Service 实现类 | **增加** `@Integrate.RequestProcessor(inOutConverterFun=...)` + `@Integrate.ResponseProcessor(serializableFun=...)` |
| 5 | **XML Serializable** | **新增文件**，同模板 6 |
| 6 | **XML InOutConverter** | **新增文件**，放在 `eip-core` 的 `thirid/{domain}/converter/` 下 |

**注意事项：**
- **Content-Type**：在 InOutConverter 中通过 `exchange.getIn().setHeader()` 设置，这是最干净的方式
- **XML 特殊字符**：构建 XML 请求体时必须转义 `&`、`<`、`>`、`"`、`'`
- **复杂 XML**：如果请求体结构复杂（深度嵌套），可使用 JAXB `Marshaller` 替代字符串拼接（参考项目中 `CxmlParserService.marshal()` 方法）
- **仅 XML 响应**：如果只需要解析 XML 响应而请求仍为 JSON，使用模板 6 即可（不需要 InOutConverter）

---

## 占位符说明

| 占位符 | 含义 | 示例 |
|--------|------|------|
| `{Vendor}` | 第三方系统名（首字母大写） | `Fedex`, `Sap`, `Kingdee` |
| `{vendor}` | 第三方系统名（首字母小写） | `fedex`, `sap`, `kingdee` |
| `{domain}` | 业务域/目录名（全小写） | `fedex`, `outOrderApi`, `tpl` |
| `{Action}` | 操作名（首字母大写） | `CreateOrder`, `QueryStatus` |
| `{ACTION_PATH}` | 常量名（全大写+下划线） | `CREATE_ORDER`, `QUERY_STATUS` |
| `{DisplayName}` | 中文显示名 | `FedEx物流`, `SAP订单` |
| `{RequestType}` | 请求类型 | `SuperMap` 或 `XxxEipTransient` |
| `{Resource}` | 资源名 | `AllProducts`, `OrderStatus` |
| `{endpoint}` | API 路径段 | `orders`, `products/create` |
