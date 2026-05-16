# 开放接口 @Open 真实代码示例

以下示例均来自当前项目 `deli-b2b-oversea` 的实际代码。

## 示例 1：外部订单创建接口（简单模式，使用内置 Token 认证）

### 接口定义

文件：`deli-aries-eip/deli-aries-eip-open-api/src/main/java/pro/shushi/deli/aries/eip/open/api/api/DeliAriesOutTradeOrderEipService.java`

```java
package pro.shushi.deli.aries.eip.open.api.api;

import pro.shushi.pamirs.meta.annotation.Fun;

@Fun(DeliAriesOutTradeOrderEipService.FUN_NAMESPACE)
public interface DeliAriesOutTradeOrderEipService {
    String FUN_NAMESPACE = "deli.aries.eip.DeliAriesOutTradeOrderEipService";
}
```

### Response DTO

文件：`deli-aries-eip/deli-aries-eip-open-api/src/main/java/pro/shushi/deli/aries/eip/open/api/tmodel/DeliAriesOutTradeOrderCommonResponse.java`

```java
package pro.shushi.deli.aries.eip.open.api.tmodel;

import pro.shushi.pamirs.meta.annotation.fun.Data;

@Data
public class DeliAriesOutTradeOrderCommonResponse {
    private String resultCode; // 返回信息 S 成功 E失败
    private String resultMsg;
}
```

### 实现类

文件：`deli-aries-eip/deli-aries-eip-core/src/main/java/pro/shushi/deli/aries/eip/core/service/DeliAriesOutTradeOrderEipServiceImpl.java`

```java
package pro.shushi.deli.aries.eip.core.service;

import org.apache.camel.ExtendedExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.shushi.deli.aries.common.constant.DeliAriesOpenApiConstants;
import pro.shushi.deli.aries.eip.api.tmodel.trade.DeliAriesOutTradeOrderEipTransient;
import pro.shushi.deli.aries.eip.open.api.api.DeliAriesOutTradeOrderEipService;
import pro.shushi.deli.aries.eip.open.api.tmodel.DeliAriesOutTradeOrderCommonResponse;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.annotation.Open;
import pro.shushi.pamirs.eip.api.constant.EipFunctionConstant;
import pro.shushi.pamirs.eip.api.entity.openapi.OpenEipResult;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.util.JsonUtils;

@Slf4j
@Service
@Fun(DeliAriesOutTradeOrderEipService.FUN_NAMESPACE)
public class DeliAriesOutTradeOrderEipServiceImpl implements DeliAriesOutTradeOrderEipService {

    @Autowired
    private DeliAriesOutTradeOrderService deliAriesOutTradeOrderService;

    @Function
    @Open(path = DeliAriesOpenApiConstants.CREATE_OUT_TRADE_ORDER)
    @Open.Advanced(
            authenticationProcessorFun = EipFunctionConstant.DEFAULT_TOKEN_AUTHENTICATION_PROCESSOR_FUN,
            authenticationProcessorNamespace = EipFunctionConstant.FUNCTION_NAMESPACE
    )
    public OpenEipResult<DeliAriesOutTradeOrderCommonResponse> createOutTradeOrder(
            IEipContext<SuperMap> context, ExtendedExchange exchange) {
        // 从 context 中解析请求数据
        DeliAriesOutTradeOrderEipTransient eipOutTradeOrder = JsonUtils.parseObject(
                JsonUtils.toJSONString(context.getInterfaceContext()),
                DeliAriesOutTradeOrderEipTransient.class);

        DeliAriesOutTradeOrderCommonResponse response = new DeliAriesOutTradeOrderCommonResponse();
        response.setResultCode("S");
        try {
            // 业务逻辑：构建并创建订单
            DeliAriesOutTradeOrder outTradeOrder = buildOutTradeOrder(eipOutTradeOrder);
            deliAriesOutTradeOrderService.create(outTradeOrder);
        } catch (Exception e) {
            response.setResultCode("E");
            response.setResultMsg(e.getMessage());
        }
        return new OpenEipResult<>(response);
    }
}
```

**关键要点**：
1. `@Open.Advanced` 未指定 `httpMethod`，使用框架默认（POST）
2. 认证使用内置的 `DEFAULT_TOKEN_AUTHENTICATION_PROCESSOR_FUN`
3. 请求数据通过 `JsonUtils.parseObject(JsonUtils.toJSONString(context.getInterfaceContext()), ...)` 解析

### Path 常量

文件：`deli-aries-common/src/main/java/pro/shushi/deli/aries/common/constant/DeliAriesOpenApiConstants.java`

```java
// 外部订单创建接口
public static final String CREATE_OUT_TRADE_ORDER = "createOutTradeOrder";
```

---

## 示例 2：ZORO 订单接口（自定义认证处理器）

### 接口定义

文件：`deli-aries-eip/deli-aries-eip-open-api/src/main/java/pro/shushi/deli/aries/eip/open/api/api/DeliAriesZoroTradeOrderEipService.java`

```java
package pro.shushi.deli.aries.eip.open.api.api;

import pro.shushi.pamirs.meta.annotation.Fun;

@Fun(DeliAriesZoroTradeOrderEipService.FUN_NAMESPACE)
public interface DeliAriesZoroTradeOrderEipService {
    String FUN_NAMESPACE = "deli.aries.eip.DeliAriesZoroTradeOrderEipService";
}
```

### 自定义认证处理器

文件：`deli-aries-eip/deli-aries-eip-core/src/main/java/pro/shushi/deli/aries/eip/core/service/zoroOrderApi/CreateZoroTradeOrderFunction.java`

```java
package pro.shushi.deli.aries.eip.core.service.zoroOrderApi;

import org.apache.camel.ExtendedExchange;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.core.common.SuperMap;
import pro.shushi.pamirs.eip.api.IEipAuthenticationProcessor;
import pro.shushi.pamirs.eip.api.IEipContext;
import pro.shushi.pamirs.eip.api.constant.EipFunctionConstant;
import pro.shushi.pamirs.meta.annotation.Fun;
import pro.shushi.pamirs.meta.annotation.Function;

@Component
@Fun(CreateZoroTradeOrderFunction.FUN_NAMESPACE)
public class CreateZoroTradeOrderFunction implements IEipAuthenticationProcessor<SuperMap> {
    public static final String FUN_NAMESPACE = "eip.zoro.CreateZoroTradeOrderFunction";
    public static final String FUN = "createZoroTradeOrder";

    @Function.fun(FUN)
    @Function.Advanced(displayName = "开放接口Token有效性认证")
    @Function(name = EipFunctionConstant.DEFAULT_TOKEN_AUTHENTICATION_PROCESSOR_FUN)
    public Boolean createZoroTradeOrder(IEipContext<SuperMap> context, ExtendedExchange exchange) {
        return authentication(context, exchange);
    }

    @Override
    public boolean authentication(IEipContext<SuperMap> context, ExtendedExchange exchange) {
        // 当前实现直接返回 true（开发阶段），生产环境需实现真实认证逻辑
        return true;
    }
}
```

### 实现类（引用自定义认证处理器）

文件：`deli-aries-eip/deli-aries-eip-core/src/main/java/pro/shushi/deli/aries/eip/core/service/zoroOrderApi/DeliAriesZoroTradeOrderEipServiceImpl.java`

```java
@Slf4j
@Service
@Fun(DeliAriesZoroTradeOrderEipService.FUN_NAMESPACE)
public class DeliAriesZoroTradeOrderEipServiceImpl implements DeliAriesZoroTradeOrderEipService {

    @Function
    @Open(path = DeliAriesOpenApiConstants.CREATE_ZORO_TRADE_ORDER)
    @Open.Advanced(
            httpMethod = "post",
            authenticationProcessorFun = CreateZoroTradeOrderFunction.FUN,
            authenticationProcessorNamespace = CreateZoroTradeOrderFunction.FUN_NAMESPACE
    )
    public OpenEipResult<DeliAriesOutTradeOrderCommonResponse> createZoroTradeOrder(
            IEipContext<SuperMap> context, ExtendedExchange exchange) {
        DeliAriesOutTradeOrderCommonResponse response = new DeliAriesOutTradeOrderCommonResponse();
        response.setResultCode("S");
        try {
            // 解析请求（支持大小写兼容的字段名）
            Map<String, Object> requestMap = JsonUtils.parseMap(
                    JsonUtils.toJSONString(context.getInterfaceContext()));
            DeliAriesZoroTradeOrderEipTransient request = new DeliAriesZoroTradeOrderEipTransient();
            request.setBeg01(getString(requestMap, "BEG01", "beg01"));
            // ... 逐字段解析 ...

            String tradeOrderCode = saveTradeOrder(request);
            response.setResultMsg(tradeOrderCode != null ? tradeOrderCode : "success");
        } catch (Exception e) {
            log.error("create zoro trade order error", e);
            response.setResultCode("E");
            response.setResultMsg(e.getMessage());
        }
        return new OpenEipResult<>(response);
    }
}
```

**关键要点**：
1. `@Open.Advanced` 显式指定 `httpMethod = "post"`
2. 认证引用自定义处理器：`CreateZoroTradeOrderFunction.FUN` 和 `CreateZoroTradeOrderFunction.FUN_NAMESPACE`
3. 请求解析使用 Map 方式，支持大小写兼容
4. 复用了示例 1 的 `DeliAriesOutTradeOrderCommonResponse` 作为 Response DTO

---

## 示例 3：EIP 初始化

文件：`deli-aries-eip/deli-aries-eip-core/src/main/java/pro/shushi/deli/aries/eip/core/thirid/init/DeliAriesEipBizInit.java`

```java
package pro.shushi.deli.aries.eip.core.thirid.init;

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
public class DeliAriesEipBizInit implements InstallDataInit, UpgradeDataInit, ReloadDataInit {

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
        return Collections.singletonList(DeliAriesEipModule.MODULE_MODULE);
    }

    @Override
    public int priority() {
        return 0;
    }

    private void initEip() {
        EipResolver.resolver(DeliAriesEipModule.MODULE_MODULE, null);
    }
}
```

**关键要点**：
1. 实现三个接口：`InstallDataInit`、`UpgradeDataInit`、`ReloadDataInit`
2. 在三个生命周期方法中都调用 `initEip()`
3. `EipResolver.resolver()` 扫描指定模块下所有带 `@Open` 注解的方法并注册
4. `modules()` 返回本类归属的模块名
5. `deli-b2b-oversea` 项目中此类已存在，新增开放接口无需重新创建

---

## Path 常量完整示例

文件：`deli-aries-common/src/main/java/pro/shushi/deli/aries/common/constant/DeliAriesOpenApiConstants.java`

```java
package pro.shushi.deli.aries.common.constant;

public class DeliAriesOpenApiConstants {
    // ... 其他常量 ...

    // 外部订单创建接口
    public static final String CREATE_OUT_TRADE_ORDER = "createOutTradeOrder";

    // ZORO订单创建接口
    public static final String CREATE_ZORO_TRADE_ORDER = "createZoroTradeOrder";
}
```

新增开放接口时，在此类末尾追加新的 path 常量即可。
