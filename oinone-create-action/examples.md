# Oinone Action & Function 代码模板与示例

## 类级别注解示例

### 使用 @Model.model 绑定模型

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderAction {
    // 此类中的 Action/Function 都绑定到 DemoOrder 模型
}
```

### 使用 @Fun 在独立类中绑定模型

```java
@Component
@Fun(DemoOrder.MODEL_MODEL)
public class DemoOrderExternalAction {
    // 通过 @Fun 绑定到 DemoOrder 模型
}
```

## ServerAction 示例

### 基本 ServerAction（单行操作）

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderAction {

    @Action(displayName = "提交订单", contextType = ActionContextTypeEnum.SINGLE,
            bindingType = ViewTypeEnum.TABLE)
    @Action.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoOrder submitOrder(DemoOrder data) {
        // 业务逻辑：修改状态
        data.setStatus(OrderStatusEnum.SUBMITTED);
        data.updateById();
        return data;
    }
}
```

### 批量操作 ServerAction

```java
@Action(displayName = "批量删除", contextType = ActionContextTypeEnum.SINGLE_AND_BATCH,
        bindingType = ViewTypeEnum.TABLE)
@Action.Advanced(type = FunctionTypeEnum.DELETE)
public List<DemoOrder> batchDelete(List<DemoOrder> dataList) {
    // 批量删除逻辑
    dataList.forEach(item -> item.deleteById());
    return dataList;
}
```

### 带显隐条件的 ServerAction

```java
@Action(displayName = "审核通过", contextType = ActionContextTypeEnum.SINGLE,
        bindingType = {ViewTypeEnum.TABLE, ViewTypeEnum.FORM})
@Action.Advanced(type = FunctionTypeEnum.UPDATE,
        invisible = "activeRecord.status != 'PENDING'")
public DemoOrder approve(DemoOrder data) {
    data.setStatus(OrderStatusEnum.APPROVED);
    data.updateById();
    return data;
}
```

### 带禁用条件的 ServerAction

```java
@Action(displayName = "编辑", contextType = ActionContextTypeEnum.SINGLE,
        bindingType = ViewTypeEnum.TABLE)
@Action.Advanced(type = FunctionTypeEnum.UPDATE,
        disable = "activeRecord.status == 'COMPLETED'")
public DemoOrder edit(DemoOrder data) {
    return data;
}
```

### 新建按钮（CONTEXT_FREE）

```java
@Action(displayName = "新建", contextType = ActionContextTypeEnum.CONTEXT_FREE,
        bindingType = ViewTypeEnum.TABLE)
@Action.Advanced(type = FunctionTypeEnum.CREATE)
public DemoOrder create(DemoOrder data) {
    data.create();
    return data;
}
```

## ViewAction 示例

### 跳转详情页

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderViewAction {

    @UxRouteButton(
        action = @UxAction(name = "redirectDetail", label = "查看详情",
            contextType = ActionContextTypeEnum.SINGLE, bindingType = ViewTypeEnum.TABLE),
        value = @UxRoute(model = DemoOrder.MODEL_MODEL, viewType = ViewTypeEnum.DETAIL)
    )
    public DemoOrder redirectDetail(DemoOrder data) {
        return data;
    }
}
```

### 跳转新建表单

```java
@UxRouteButton(
    action = @UxAction(name = "redirectCreate", label = "新建",
        contextType = ActionContextTypeEnum.CONTEXT_FREE, bindingType = ViewTypeEnum.TABLE),
    value = @UxRoute(model = DemoOrder.MODEL_MODEL, viewType = ViewTypeEnum.FORM,
        load = "construct")
)
public DemoOrder redirectCreate(DemoOrder data) {
    return data;
}
```

### 跳转编辑表单

```java
@UxRouteButton(
    action = @UxAction(name = "redirectEdit", label = "编辑",
        contextType = ActionContextTypeEnum.SINGLE, bindingType = ViewTypeEnum.TABLE),
    value = @UxRoute(model = DemoOrder.MODEL_MODEL, viewType = ViewTypeEnum.FORM,
        load = "queryOne")
)
public DemoOrder redirectEdit(DemoOrder data) {
    return data;
}
```

### 带过滤条件的跳转

```java
@UxRouteButton(
    action = @UxAction(name = "redirectActiveOrders", label = "查看有效订单",
        contextType = ActionContextTypeEnum.CONTEXT_FREE, bindingType = ViewTypeEnum.TABLE),
    value = @UxRoute(model = DemoOrder.MODEL_MODEL, viewType = ViewTypeEnum.TABLE,
        domain = "status == 'ACTIVE'")
)
public DemoOrder redirectActiveOrders(DemoOrder data) {
    return data;
}
```

## UrlAction 示例

### 打开外部文档链接

```java
@UxLinkButton(
    action = @UxAction(name = "openDoc", label = "查看文档",
        contextType = ActionContextTypeEnum.SINGLE, bindingType = ViewTypeEnum.TABLE),
    value = @UxLink(value = "https://guide.oinone.top/doc/${activeRecord.code}",
        openType = ActionTargetEnum.OPEN_WINDOW)
)
public DemoOrder openDoc(DemoOrder data) {
    return data;
}
```

## ClientAction 示例

### 表单校验按钮

```java
@UxClientButton(
    action = @UxAction(name = "validateForm", label = "校验表单",
        contextType = ActionContextTypeEnum.CONTEXT_FREE, bindingType = ViewTypeEnum.FORM),
    value = @UxClient("$$internal_ValidateForm")
)
public DemoOrder validateForm(DemoOrder data) {
    return data;
}
```

### 刷新页面按钮

```java
@UxClientButton(
    action = @UxAction(name = "refreshPage", label = "刷新",
        contextType = ActionContextTypeEnum.CONTEXT_FREE, bindingType = ViewTypeEnum.TABLE),
    value = @UxClient("$$internal_ReloadData")
)
public DemoOrder refreshPage(DemoOrder data) {
    return data;
}
```

### 关闭弹窗按钮

```java
@UxClientButton(
    action = @UxAction(name = "cancelDialog", label = "取消",
        contextType = ActionContextTypeEnum.CONTEXT_FREE, bindingType = ViewTypeEnum.FORM),
    value = @UxClient("$$internal_DialogCancel")
)
public DemoOrder cancelDialog(DemoOrder data) {
    return data;
}
```

## Function 示例

### 基本 Function（开放 API）

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderFunction {

    @Function(openLevel = {FunctionOpenEnum.LOCAL, FunctionOpenEnum.REMOTE, FunctionOpenEnum.API})
    @Function.Advanced(type = FunctionTypeEnum.QUERY, displayName = "查询订单详情")
    public DemoOrder queryDetail(DemoOrder data) {
        // 查询逻辑
        return data.queryById();
    }
}
```

### 分页查询 Function

```java
@Function(openLevel = {FunctionOpenEnum.LOCAL, FunctionOpenEnum.REMOTE, FunctionOpenEnum.API})
@Function.Advanced(type = FunctionTypeEnum.QUERY, displayName = "分页查询订单")
public Pagination<DemoOrder> queryPage(Pagination<DemoOrder> page, IWrapper<DemoOrder> queryWrapper) {
    // 分页查询逻辑
    return new DemoOrder().queryPage(page, queryWrapper);
}
```

### 仅内部调用的 Function

```java
@Function(openLevel = FunctionOpenEnum.LOCAL)
@Function.Advanced(type = FunctionTypeEnum.UPDATE, displayName = "内部状态更新")
public DemoOrder updateStatusInternal(DemoOrder data) {
    // 仅供服务端内部调用
    return data;
}
```

### 使用 @Fun 在独立类中定义 Function

```java
@Component
@Fun(DemoOrder.MODEL_MODEL)
public class DemoOrderFunctions {

    @Function(openLevel = FunctionOpenEnum.API)
    @Function.Advanced(type = FunctionTypeEnum.QUERY, displayName = "加载订单详情")
    public DemoOrder loadDetail(DemoOrder data) {
        // 加载详情逻辑
        return data;
    }
}
```

### 使用传输模型作为额外入参

```java
@Function(openLevel = {FunctionOpenEnum.LOCAL, FunctionOpenEnum.REMOTE, FunctionOpenEnum.API})
@Function.Advanced(type = FunctionTypeEnum.QUERY, displayName = "条件查询订单")
public DemoOrder queryWithCondition(DemoOrder data, DemoQueryRequest request) {
    // 使用 request 中的额外条件进行查询
    return data;
}
```

## ExtPoint 示例

### 实现快捷扩展点接口（创建前）

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderExtPoint implements CreateBeforeExtPoint<DemoOrder> {

    @Override
    @ExtPoint.Implement(expression = "context.model == '" + DemoOrder.MODEL_MODEL + "'")
    public DemoOrder beforeCreate(DemoOrder data) {
        // 创建前的业务逻辑，如设置默认值
        if (data.getStatus() == null) {
            data.setStatus(OrderStatusEnum.DRAFT);
        }
        return data;
    }
}
```

### 自定义扩展点

```java
// 1. 定义扩展点接口
@Ext(DemoOrder.class)
public interface DemoOrderExtPoint {

    @ExtPoint(displayName = "订单审核扩展")
    DemoOrder auditOrder(DemoOrder data);
}

// 2. 实现扩展点
@Component
@Ext(DemoOrder.class)
public class DemoOrderExtPointImpl implements DemoOrderExtPoint {

    @Override
    @ExtPoint.Implement(priority = 50)
    public DemoOrder auditOrder(DemoOrder data) {
        // 扩展逻辑
        return data;
    }
}

// 3. 调用扩展点
Ext.run(DemoOrderExtPoint::auditOrder, new Object[]{data});
```

## Hook 示例

### HookBefore（前置拦截，修改入参）

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderHookBefore implements HookBefore {

    @Override
    @Hook(model = {DemoOrder.MODEL_MODEL}, fun = {"create"},
          priority = 50, active = true)
    public Object[] run(Object[] args) {
        // 修改入参
        DemoOrder data = (DemoOrder) args[0];
        data.setCreateTime(new Date());
        args[0] = data;
        return args;
    }
}
```

### HookAfter（后置拦截，修改返回值）

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderHookAfter implements HookAfter {

    @Override
    @Hook(model = {DemoOrder.MODEL_MODEL}, fun = {"create"},
          priority = 50, active = true)
    public Object run(Object[] args, Object result) {
        // 修改返回值或执行后续操作
        DemoOrder data = (DemoOrder) result;
        // 发送通知等
        return data;
    }
}
```

## Trigger 示例

### 数据创建后触发

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderTrigger {

    @Trigger(displayName = "订单创建后触发",
             name = DemoOrder.MODEL_MODEL + "#Trigger#onCreate",
             condition = TriggerConditionEnum.ON_CREATE)
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE)
    public DemoOrder onOrderCreated(DemoOrder data) {
        // 订单创建后的异步逻辑
        return data;
    }
}
```

## Schedule 示例

### Cron 定时任务

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderSchedule {

    @XSchedule(cron = "0 0 2 * * ?")
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE, displayName = "每日凌晨2点清理过期订单")
    public void cleanExpiredOrders() {
        // 定时清理逻辑
    }
}
```

## Async 示例

### 异步执行带重试

```java
@Component
@Model.model(DemoOrder.MODEL_MODEL)
public class DemoOrderAsync {

    @XAsync(displayName = "异步发送订单通知", limitRetryNumber = 3,
            nextRetryTimeValue = 60, nextRetryTimeUnit = TimeUnitEnum.SECOND)
    @Function(openLevel = FunctionOpenEnum.LOCAL)
    @Function.Advanced(type = FunctionTypeEnum.UPDATE, displayName = "发送订单通知")
    public DemoOrder sendNotification(DemoOrder data) {
        // 异步执行的通知逻辑
        return data;
    }
}
```

## Transaction 示例

### 声明式事务

```java
@PamirsTransactional
@Function(openLevel = FunctionOpenEnum.LOCAL)
@Function.Advanced(type = FunctionTypeEnum.UPDATE)
public DemoOrder transferOrder(DemoOrder data) {
    // 在同一事务中执行多个操作
    return data;
}
```

### 编程式事务

```java
Tx.build(new TxConfig().setPropagation(Propagation.REQUIRED.value()))
    .executeWithoutResult(status -> {
        // 事务内的业务逻辑
    });
```

## Validation 示例

### 表达式规则校验

```java
@Validation(ruleWithTips = {
    @Validation.Rule(value = "!IS_BLANK(name)", error = "名称不能为空"),
    @Validation.Rule(value = "amount > 0", error = "金额必须大于0"),
    @Validation.Rule(value = "LEN(name) <= 100", error = "名称长度不能超过100")
})
@Action(displayName = "提交订单")
@Action.Advanced(type = FunctionTypeEnum.UPDATE)
public DemoOrder submitOrder(DemoOrder data) {
    return data;
}
```

### 引用校验函数

```java
@Validation(check = "checkOrderData")
@Action(displayName = "确认订单")
public DemoOrder confirmOrder(DemoOrder data) {
    return data;
}

// 校验函数需在同模型中定义
@Function(openLevel = FunctionOpenEnum.LOCAL)
@Function.Advanced(type = FunctionTypeEnum.QUERY)
public Boolean checkOrderData(DemoOrder data) {
    if (data.getAmount() == null || data.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new PamirsException("订单金额必须大于0");
    }
    return true;
}
```

## Expression 示例

### invisible / disable 表达式

```java
// 仅草稿状态显示"提交"按钮
@Action.Advanced(invisible = "activeRecord.status != 'DRAFT'")

// 已完成状态禁用"编辑"按钮
@Action.Advanced(disable = "activeRecord.status == 'COMPLETED'")

// 无 ID 时隐藏（新建场景）
@Action.Advanced(invisible = "!activeRecord.id")
```
