# Oinone 枚举代码模板与示例

## 标准枚举（IEnum\<String\>）

最常用的枚举类型，值为字符串，适合状态、类型等单选场景。

```java
package pro.shushi.pamirs.demo.api.enumeration;

import pro.shushi.pamirs.meta.annotation.Dict;
import pro.shushi.pamirs.meta.common.enmu.IEnum;

@Dict(dictionary = OrderStatusEnum.DICTIONARY, displayName = "订单状态")
public enum OrderStatusEnum implements IEnum<String> {

    DRAFT("DRAFT", "草稿", "订单草稿状态"),
    CONFIRMED("CONFIRMED", "已确认", "订单已确认"),
    SHIPPED("SHIPPED", "已发货", "订单已发货"),
    COMPLETED("COMPLETED", "已完成", "订单已完成"),
    CANCELLED("CANCELLED", "已取消", "订单已取消");

    public static final String DICTIONARY = "demo.OrderStatusEnum";

    private final String value;
    private final String displayName;
    private final String help;

    OrderStatusEnum(String value, String displayName, String help) {
        this.value = value;
        this.displayName = displayName;
        this.help = help;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String help() {
        return help;
    }
}
```

## 整数枚举（IEnum\<Integer\>）

值为整数，适合需要数值排序或计算的场景。

```java
@Dict(dictionary = PriorityEnum.DICTIONARY, displayName = "优先级")
public enum PriorityEnum implements IEnum<Integer> {

    LOW(1, "低", "低优先级"),
    MEDIUM(2, "中", "中优先级"),
    HIGH(3, "高", "高优先级"),
    URGENT(4, "紧急", "紧急优先级");

    public static final String DICTIONARY = "demo.PriorityEnum";

    private final Integer value;
    private final String displayName;
    private final String help;

    PriorityEnum(Integer value, String displayName, String help) {
        this.value = value;
        this.displayName = displayName;
        this.help = help;
    }

    @Override
    public Integer value() {
        return value;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String help() {
        return help;
    }
}
```

## 位运算枚举（BitEnum）

值必须是 2 的幂次方，适合多选标记、权限组合等需要按位组合的场景。

```java
import pro.shushi.pamirs.meta.annotation.Dict;
import pro.shushi.pamirs.meta.common.enmu.BitEnum;

@Dict(dictionary = PermissionEnum.DICTIONARY, displayName = "权限类型")
public enum PermissionEnum implements BitEnum {

    READ(1L, "读取", "读取权限"),
    WRITE(2L, "写入", "写入权限"),
    DELETE(4L, "删除", "删除权限"),
    ADMIN(8L, "管理", "管理权限");

    public static final String DICTIONARY = "demo.PermissionEnum";

    private final Long value;
    private final String displayName;
    private final String help;

    PermissionEnum(Long value, String displayName, String help) {
        this.value = value;
        this.displayName = displayName;
        this.help = help;
    }

    @Override
    public Long value() {
        return value;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String help() {
        return help;
    }
}
```

## 可继承枚举（BaseEnum\<E, String\>）

使用 Java class（非 enum）实现，子类可以继承父类的所有枚举值并添加新值。

```java
package pro.shushi.pamirs.demo.api.enumeration;

import pro.shushi.pamirs.meta.annotation.Dict;
import pro.shushi.pamirs.meta.common.enmu.BaseEnum;

@Dict(dictionary = BizChannelEnum.DICTIONARY, displayName = "业务渠道")
public class BizChannelEnum extends BaseEnum<BizChannelEnum, String> {

    private static final long serialVersionUID = 1L;

    public static final String DICTIONARY = "demo.BizChannelEnum";

    public static final BizChannelEnum DEFAULT = create("DEFAULT", "DEFAULT", "默认渠道", "默认渠道");
    public static final BizChannelEnum ONLINE = create("ONLINE", "ONLINE", "线上渠道", "线上渠道");
    public static final BizChannelEnum OFFLINE = create("OFFLINE", "OFFLINE", "线下渠道", "线下渠道");
}
```

### 子类继承扩展

子类自动继承父类的所有枚举值，并可添加新值。

```java
@Dict(dictionary = ExtBizChannelEnum.DICTIONARY, displayName = "扩展业务渠道")
public class ExtBizChannelEnum extends BizChannelEnum {

    private static final long serialVersionUID = 1L;

    public static final String DICTIONARY = "demo.ExtBizChannelEnum";

    // 继承了父类的 DEFAULT、ONLINE、OFFLINE，同时新增：
    public static final ExtBizChannelEnum WECHAT = create("WECHAT", "WECHAT", "微信渠道", "微信渠道");
    public static final ExtBizChannelEnum ALIPAY = create("ALIPAY", "ALIPAY", "支付宝渠道", "支付宝渠道");
}
```

### ref() — 显式引用父类枚举值

子类可通过 `ref()` 显式引用父类的枚举值，而非自动继承全部。

```java
@Dict(dictionary = "demo.ExtType", displayName = "扩展类型")
public final class ExtTypeEnum extends BaseTypeEnum {

    private static final long serialVersionUID = 1L;

    // 通过 ref() 显式引用父类枚举值
    public static final ExtTypeEnum TYPE_A = ref(BaseTypeEnum.TYPE_A);
    public static final ExtTypeEnum TYPE_B = ref(BaseTypeEnum.TYPE_B);

    // 新增自己的枚举值
    public static final ExtTypeEnum TYPE_C = create("TYPE_C", "TYPE_C", "类型C", "类型C");
}
```

## 可继承位运算枚举（BaseEnum + BitEnum）

可继承枚举实现 `BitEnum` 接口，用于需要继承扩展的多选场景。

```java
import pro.shushi.pamirs.meta.annotation.Dict;
import pro.shushi.pamirs.meta.common.enmu.BaseEnum;
import pro.shushi.pamirs.meta.common.enmu.BitEnum;

@Dict(dictionary = PartnerOptionEnum.DICTIONARY, displayName = "合作伙伴标记")
public class PartnerOptionEnum extends BaseEnum<PartnerOptionEnum, Long> implements BitEnum {

    private static final long serialVersionUID = 1L;

    public static final String DICTIONARY = "demo.PartnerOptionEnum";

    public static final PartnerOptionEnum ORGANIZATION = create("ORGANIZATION", 1L << 0, "组织架构", "组织架构管理标记");
    public static final PartnerOptionEnum SALE = create("SALE", 1L << 1, "销售", "销售标记");
    public static final PartnerOptionEnum SUPPLIER = create("SUPPLIER", 1L << 2, "供货", "供货标记");
}
```

### BaseEnum create() 工厂方法签名

```java
// 完整四参数（推荐）
public static <E extends BaseEnum> E create(String name, T value, String displayName, String help)

// 带扩展属性
public static <E extends BaseEnum> E create(String name, T value, String displayName, String help, Map<String, Object> attributes)
```

## 带自定义属性的枚举

枚举可以携带额外的业务属性，在标准三要素之外追加自定义字段。

```java
@Dict(dictionary = AiModelVendorType.DICTIONARY, displayName = "大模型厂商")
public enum AiModelVendorType implements IEnum<String> {

    OPENAI("OPENAI", "OpenAI", "OpenAI", "https://api.openai.com"),
    QWEN("QWEN", "Qwen", "通义千问", "https://dashscope.aliyuncs.com"),
    DEEPSEEK("DEEPSEEK", "DeepSeek", "DeepSeek", "https://api.deepseek.com");

    public static final String DICTIONARY = "demo.AiModelVendorType";

    private final String value;
    private final String displayName;
    private final String help;
    private final String baseUrl;

    AiModelVendorType(String value, String displayName, String help, String baseUrl) {
        this.value = value;
        this.displayName = displayName;
        this.help = help;
        this.baseUrl = baseUrl;
    }

    @Override
    public String value() { return value; }

    @Override
    public String displayName() { return displayName; }

    @Override
    public String help() { return help; }

    public String getBaseUrl() { return baseUrl; }
}
```

## 错误码枚举（ExpBaseEnum + @Errors）

错误码枚举用于定义业务/系统错误码，使用 `@Errors` 注解（而非 `@Dict`），实现 `ExpBaseEnum` 接口。三要素为 `(ERROR_TYPE, code, msg)`。

```java
import pro.shushi.pamirs.meta.annotation.Errors;
import pro.shushi.pamirs.meta.common.enmu.ExpBaseEnum;

@Errors(displayName = "订单模块错误枚举")
public enum DemoOrderExp implements ExpBaseEnum {

    ORDER_NOT_FOUND(ERROR_TYPE.BIZ_ERROR, 10010001, "订单不存在"),
    ORDER_STATUS_INVALID(ERROR_TYPE.BIZ_ERROR, 10010002, "订单状态不合法"),
    ORDER_AMOUNT_ERROR(ERROR_TYPE.DATA_ERROR, 10010003, "订单金额异常");

    private final ERROR_TYPE type;
    private final int code;
    private final String msg;

    DemoOrderExp(ERROR_TYPE type, int code, String msg) {
        this.type = type;
        this.code = code;
        this.msg = msg;
    }

    @Override
    public ERROR_TYPE type() { return type; }

    @Override
    public int code() { return code; }

    @Override
    public String msg() { return msg; }

    @Override
    public Integer value() { return code; }

    @Override
    public String displayName() { return msg; }
}
```

## BaseEnum 高级用法

### switches() / switchGet() — 流式匹配

BaseEnum 提供流式 switch/case API，替代 Java switch 语句。

```java
// 无返回值的 switch
BaseEnum.switches(channelEnum, BaseEnum.caseValue(),
    BaseEnum.cases(BizChannelEnum.ONLINE).to(() -> {
        // 处理线上渠道
    }),
    BaseEnum.cases(BizChannelEnum.OFFLINE).to(() -> {
        // 处理线下渠道
    }),
    BaseEnum.defaults(() -> {
        // 默认处理
    })
);

// 有返回值的 switch
String result = BaseEnum.switchGet(channelEnum,
    BaseEnum.cases(BizChannelEnum.ONLINE).to(() -> "线上"),
    BaseEnum.cases(BizChannelEnum.OFFLINE).to(() -> "线下"),
    BaseEnum.defaults(() -> "未知")
);
```

## 枚举引用其他枚举作为父分类

枚举项可以携带另一个枚举作为父级/分类属性，实现层级关系。

```java
@Dict(dictionary = OrderSubTypeEnum.DICTIONARY, displayName = "订单子类型")
public enum OrderSubTypeEnum implements IEnum<String> {

    NORMAL_SALE("NORMAL_SALE", "普通销售", "普通销售订单", OrderTypeEnum.SALE),
    PRESALE("PRESALE", "预售", "预售订单", OrderTypeEnum.SALE),
    NORMAL_RETURN("NORMAL_RETURN", "普通退货", "普通退货单", OrderTypeEnum.RETURN);

    public static final String DICTIONARY = "demo.OrderSubTypeEnum";

    private final String value;
    private final String displayName;
    private final String help;
    private final OrderTypeEnum parent;

    OrderSubTypeEnum(String value, String displayName, String help, OrderTypeEnum parent) {
        this.value = value;
        this.displayName = displayName;
        this.help = help;
        this.parent = parent;
    }

    @Override
    public String value() { return value; }
    @Override
    public String displayName() { return displayName; }
    @Override
    public String help() { return help; }

    public OrderTypeEnum getParent() { return parent; }
}
```

## 模型字段中引用枚举

### 单选枚举字段

```java
@Field(displayName = "订单状态")
@Field.Enum
private OrderStatusEnum status;

// 带默认值
@Field(displayName = "优先级", defaultValue = "MEDIUM")
@Field.Enum
private PriorityEnum priority;
```

### 多选枚举字段（BitEnum）

BitEnum 类型的字段使用 `List<枚举>` 类型。

```java
@Field(displayName = "权限列表", multi = true)
@Field.Enum
private List<PermissionEnum> permissions;

// 带默认值（位运算组合值）
@Field(displayName = "客户端类型", multi = true, defaultValue = "3")
@Field.Enum
private List<ClientTypeEnum> clientTypes;
```

## 数据字典编码示例

```java
// 示例
public static final String DICTIONARY = "demo.OrderStatusEnum";
public static final String DICTIONARY = "ai.AiStreamEventType";
public static final String DICTIONARY = "base.ModelType";
public static final String DICTIONARY = "ui.designer.WidgetSpanEnum";
```
