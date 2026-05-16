# Oinone 函数拦截器 代码模板与示例

## HookBefore — 前置拦截器

### 基本示例（带 model + fun 过滤）

```java
package pro.shushi.deli.aries.trade.core.hook;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookBefore;
import pro.shushi.pamirs.meta.api.dto.fun.Function;
import pro.shushi.pamirs.meta.common.exception.PamirsException;

/**
 * 订单创建前置校验拦截器
 * 在订单创建前校验必填字段
 *
 * 注意：
 * - 后端编程式调用默认不触发 Hook，需 PamirsSession.directive().enableHook()
 * - 本拦截器所在模块必须以 jar 依赖方式被 boot 模块引入
 */
@Slf4j
@Component
public class OrderCreateValidateHookBefore implements HookBefore {

    @Override
    @Hook(priority = 100,
          model = {DemoOrder.MODEL_MODEL},
          fun = "create")
    public Object run(Function function, Object... args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }

        DemoOrder order = (DemoOrder) args[0];
        log.info("订单创建前置校验，订单编号: {}", order.getCode());

        // 校验逻辑
        if (order.getBuyerName() == null || order.getBuyerName().isEmpty()) {
            throw PamirsException.construct(DemoExpEnumerate.BUYER_NAME_REQUIRED).errThrow();
        }

        return null;
    }
}
```

### 带 module 过滤的示例

当拦截器只需要在特定入口模块的请求中生效时，使用 `module` 参数过滤：

```java
package pro.shushi.deli.aries.core.mall.hook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookBefore;
import pro.shushi.pamirs.meta.api.dto.fun.Function;
import pro.shushi.pamirs.meta.common.exception.PamirsException;

/**
 * 商城会话校验拦截器
 * 仅在商城模块入口的请求中生效，校验买卖双方合作关系
 *
 * 注意：module 指的是"请求入口模块"（前端 URL 中的模块名），
 * 而非本拦截器所在的模块
 */
@Slf4j
@Component
public class MallSessionValidateHookBefore implements HookBefore {

    @Autowired
    private PartnerRelationService partnerRelationService;

    @Override
    @Hook(priority = 100,
          module = {MallModule.MODULE_MODULE, MerchantMallModule.MODULE_MODULE},
          model = {DemoOrder.MODEL_MODEL},
          fun = "create")
    public Object run(Function function, Object... args) {
        String module = function.getModule();
        log.info("商城会话校验，请求入口模块: {}", module);

        // 校验买卖双方合作关系
        PartnerRelation relation = partnerRelationService.queryActiveRelation(
                MallSession.getSellerCode(), MallSession.getBuyerCode());
        if (relation == null) {
            throw PamirsException.construct(DemoExpEnumerate.PARTNER_RELATION_NOT_FOUND).errThrow();
        }

        return null;
    }
}
```

### 修改入参的示例

```java
package pro.shushi.deli.aries.item.core.hook;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookBefore;
import pro.shushi.pamirs.meta.api.dto.fun.Function;

/**
 * 商品查询前置处理拦截器
 * 在商品分页查询前自动注入当前商家过滤条件
 */
@Slf4j
@Component
public class ItemQueryFilterHookBefore implements HookBefore {

    @Override
    @Hook(priority = 100,
          model = {DemoItem.MODEL_MODEL},
          fun = "queryPage")
    public Object run(Function function, Object... args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }

        // 修改查询条件，注入当前商家过滤
        DemoItem query = (DemoItem) args[0];
        if (query.getMerchantCode() == null) {
            query.setMerchantCode(MerchantSession.getCurrentMerchantCode());
            log.info("自动注入商家过滤条件: {}", query.getMerchantCode());
        }

        // 修改后的 query 会作为入参传递给被拦截函数
        return null;
    }
}
```

## HookAfter — 后置拦截器

### 基本示例（带 Object[] 解包）

```java
package pro.shushi.deli.aries.trade.core.hook;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookAfter;
import pro.shushi.pamirs.meta.api.dto.fun.Function;

/**
 * 订单查询后置处理拦截器
 * 在订单详情查询后，补充计算字段
 *
 * 注意：ret 可能是 Object[] 类型，必须做类型判断后再处理
 */
@Slf4j
@Component
public class OrderQueryEnrichHookAfter implements HookAfter {

    @Override
    @Hook(priority = 100,
          model = {DemoOrder.MODEL_MODEL},
          fun = "queryOne")
    public Object run(Function function, Object ret) {
        if (ret == null) {
            return null;
        }

        // 关键：Object[] 解包处理
        Object data = null;
        if (ret instanceof Object[]) {
            Object[] rets = (Object[]) ret;
            if (rets.length == 1) {
                data = rets[0];
            }
        } else {
            data = ret;
        }

        if (data == null) {
            return ret;
        }

        // 业务处理
        if (data instanceof DemoOrder) {
            DemoOrder order = (DemoOrder) data;
            log.info("订单查询后置处理，订单ID: {}", order.getId());
            // 补充计算字段
            order.setTotalAmountDisplay(formatAmount(order.getTotalAmount()));
        }

        return ret;
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, java.math.BigDecimal.ROUND_HALF_UP).toPlainString();
    }
}
```

### 修改分页查询返回值的示例

```java
package pro.shushi.deli.aries.major.core.hook;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookAfter;
import pro.shushi.pamirs.meta.api.dto.condition.Pagination;
import pro.shushi.pamirs.meta.api.dto.fun.Function;

/**
 * 用户分页查询后置处理拦截器
 * 在用户列表查询后，脱敏手机号
 */
@Slf4j
@Component
public class UserQueryPageMaskHookAfter implements HookAfter {

    @Override
    @Hook(priority = 100,
          model = {DemoUser.MODEL_MODEL},
          fun = "queryPage")
    public Object run(Function function, Object ret) {
        if (ret == null) {
            return null;
        }

        // Object[] 解包
        Object data = null;
        if (ret instanceof Object[]) {
            Object[] rets = (Object[]) ret;
            if (rets.length == 1) {
                data = rets[0];
            }
        } else {
            data = ret;
        }

        if (data == null || !(data instanceof Pagination)) {
            return ret;
        }

        // 处理分页数据
        Pagination<?> pagination = (Pagination<?>) data;
        List<?> content = pagination.getContent();
        if (CollectionUtils.isEmpty(content)) {
            return ret;
        }

        for (Object item : content) {
            if (item instanceof DemoUser) {
                DemoUser user = (DemoUser) item;
                // 手机号脱敏
                if (user.getPhone() != null && user.getPhone().length() == 11) {
                    user.setPhone(user.getPhone().substring(0, 3) + "****" + user.getPhone().substring(7));
                }
            }
        }

        log.info("用户列表脱敏处理完成，共处理 {} 条记录", content.size());
        return ret;
    }
}
```

### 后置拦截器记录审计日志的示例

```java
package pro.shushi.deli.aries.trade.core.hook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookAfter;
import pro.shushi.pamirs.meta.api.dto.fun.Function;
import pro.shushi.pamirs.meta.api.session.PamirsSession;

/**
 * 订单操作审计日志拦截器
 * 在订单创建/更新后记录操作日志
 */
@Slf4j
@Component
public class OrderAuditLogHookAfter implements HookAfter {

    @Autowired
    private AuditLogService auditLogService;

    @Override
    @Hook(priority = Integer.MAX_VALUE,
          model = {DemoOrder.MODEL_MODEL},
          fun = "create",
          displayName = "订单创建审计日志")
    public Object run(Function function, Object ret) {
        if (ret == null) {
            return null;
        }

        try {
            // Object[] 解包
            Object data = null;
            if (ret instanceof Object[]) {
                Object[] rets = (Object[]) ret;
                if (rets.length == 1) {
                    data = rets[0];
                }
            } else {
                data = ret;
            }

            if (data instanceof DemoOrder) {
                DemoOrder order = (DemoOrder) data;
                auditLogService.log(
                        PamirsSession.getUserId(),
                        "ORDER_CREATE",
                        order.getId(),
                        "创建订单: " + order.getCode()
                );
                log.info("订单审计日志已记录，订单ID: {}", order.getId());
            }
        } catch (Exception e) {
            // 审计日志失败不应影响主流程
            log.error("记录订单审计日志异常", e);
        }

        return ret;
    }
}
```

## 骨架模板

当用户只描述了目的但未提供具体技术信息时，生成以下骨架代码，用 TODO 标注需要用户填充的部分。

### HookBefore 骨架

```java
package pro.shushi.deli.aries.{module}.core.hook;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookBefore;
import pro.shushi.pamirs.meta.api.dto.fun.Function;

/**
 * TODO: 补充拦截器的业务说明
 *
 * 注意事项：
 * - 后端编程式调用默认不触发 Hook，需 PamirsSession.directive().enableHook()
 * - 本拦截器所在模块必须以 jar 依赖方式被 boot 模块引入
 * - model + fun 过滤属性不可为空，否则会对所有函数生效
 */
@Slf4j
@Component
public class TargetModelFeatureHookBefore implements HookBefore {

    // TODO: 注入需要的服务
    // @Autowired
    // private XxxService xxxService;

    @Override
    @Hook(priority = 100,
          model = {TargetModel.MODEL_MODEL},  // TODO: 替换为实际的目标模型
          fun = "targetFunction")              // TODO: 替换为实际的目标函数编码
    public Object run(Function function, Object... args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }

        TargetModel data = (TargetModel) args[0]; // TODO: 替换为实际的模型类型
        log.info("前置拦截器执行，数据ID: {}", data.getId());

        try {
            // TODO: 在此编写前置拦截逻辑
            //
            // 常见场景：
            // 1. 参数校验（校验失败抛 PamirsException）
            // 2. 数据补全（自动填充默认值）
            // 3. 权限校验（检查当前用户是否有操作权限）
            // 4. 修改入参（注入过滤条件等）

        } catch (Exception e) {
            log.error("前置拦截器执行异常", e);
            throw e;
        }

        return null;
    }
}
```

### HookAfter 骨架

```java
package pro.shushi.deli.aries.{module}.core.hook;

import org.springframework.stereotype.Component;
import pro.shushi.pamirs.meta.annotation.Hook;
import pro.shushi.pamirs.meta.annotation.fun.extern.Slf4j;
import pro.shushi.pamirs.meta.api.core.faas.HookAfter;
import pro.shushi.pamirs.meta.api.dto.fun.Function;

/**
 * TODO: 补充拦截器的业务说明
 *
 * 注意事项：
 * - ret 可能是 Object[] 类型，必须做类型判断后再处理
 * - 后端编程式调用默认不触发 Hook，需 PamirsSession.directive().enableHook()
 * - 本拦截器所在模块必须以 jar 依赖方式被 boot 模块引入
 * - model + fun 过滤属性不可为空，否则会对所有函数生效
 */
@Slf4j
@Component
public class TargetModelFeatureHookAfter implements HookAfter {

    // TODO: 注入需要的服务
    // @Autowired
    // private XxxService xxxService;

    @Override
    @Hook(priority = 100,
          model = {TargetModel.MODEL_MODEL},  // TODO: 替换为实际的目标模型
          fun = "targetFunction")              // TODO: 替换为实际的目标函数编码
    public Object run(Function function, Object ret) {
        if (ret == null) {
            return null;
        }

        // 关键：Object[] 解包处理
        Object data = null;
        if (ret instanceof Object[]) {
            Object[] rets = (Object[]) ret;
            if (rets.length == 1) {
                data = rets[0];
            }
        } else {
            data = ret;
        }

        if (data == null) {
            return ret;
        }

        log.info("后置拦截器执行，函数: {}", function.getFun());

        try {
            // TODO: 在此编写后置拦截逻辑
            //
            // 常见场景：
            // 1. 数据增强（补充计算字段、关联数据）
            // 2. 数据脱敏（隐藏敏感信息）
            // 3. 审计日志（记录操作日志）
            // 4. 修改返回值（格式转换等）

            if (data instanceof TargetModel) { // TODO: 替换为实际的模型类型
                TargetModel model = (TargetModel) data;
                // TODO: 业务处理
            }

        } catch (Exception e) {
            // 后置拦截器异常通常不应阻断主流程
            log.error("后置拦截器执行异常", e);
        }

        return ret;
    }
}
```