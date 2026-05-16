
# Oinone GraphQL 生成规则

## Overview

Oinone 框架的 GraphQL 采用**嵌套结构**，所有操作必须包裹在模型对应的 wrapper 中，参数直接内联（不使用 `$variables`）。这与标准 GraphQL 的扁平格式完全不同。

## 核心规则

### 1. Wrapper 命名公式

```
wrapper = lowerFirst(lastSegment(MODEL_MODEL)) + "Mutation" | "Query"
```

- `lastSegment` = `@Model.model()` 值最后一个 `.` 后的字符串
- `lowerFirst` = 首字母小写
- 写操作（`@Action`）→ `Mutation` 后缀
- 读操作（默认 CRUD / `@Function` QUERY）→ `Query` 后缀

**示例推导**：
| `@Model.model()` | lastSegment | Query Wrapper | Mutation Wrapper |
|---|---|---|---|
| `market.DistributionBinding` | `DistributionBinding` | `distributionBindingQuery` | `distributionBindingMutation` |
| `pamirs.market.MarketOrder` | `MarketOrder` | `marketOrderQuery` | `marketOrderMutation` |

### 2. 参数规则

- Java 方法参数名 → GraphQL 参数名（一一对应）
- 参数值**内联**传递，**不使用** `$variables`
- `@Action` 方法的参数类型通常是绑定的模型对象本身

### 3. 格式模板

```graphql
# 写操作（@Action 方法）
mutation {
  <wrapperMutation> {
    <methodName>(<paramName>: { field1: value1, field2: value2 }) {
      responseField1
      responseField2
    }
  }
}

# 读操作（默认分页查询）
query {
  <wrapperQuery> {
    queryPage(page: { currentPage: 1, size: 20 }, queryWrapper: { rsql: "field==value" }) {
      content { field1 field2 }
      totalElements
      totalPages
    }
  }
}

# 读操作（单条查询）
query {
  <wrapperQuery> {
    queryOne(queryWrapper: { rsql: "id==12345" }) {
      field1
      field2
    }
  }
}
```

### 4. 多个 Action 类绑定同一模型

多个 Action 类可以绑定同一个 `@Model.model()`，它们的方法共享同一个 GraphQL wrapper。例如 `DistributionAccountAction`、`DistributorConsoleAction`、`AdminRiskAction` 都绑定 `DistributionAccount`，所有方法都在 `distributionAccountMutation` 下。

## 常见错误

| 错误写法 | 正确写法 |
|---|---|
| `mutation CaptureInviter($binding: ...) { captureInviter(...) }` | `mutation { distributionBindingMutation { captureInviter(binding: {...}) {...} } }` |
| `query { queryPage(...) }` | `query { distributionBindingQuery { queryPage(...) {...} } }` |
| 使用 `$variables` 引用参数 | 参数值直接内联到花括号中 |
| wrapper 首字母大写 | wrapper 首字母必须小写（`distributionBindingMutation`） |

## 快速参考

→ 本项目完整的模型 → wrapper 映射表、Action → 方法签名详见 [reference.md](reference.md)

---

## 补充参考资料

如需更详细的 API / 示例 / 模板资源，请使用 Read 工具读取以下文件（路径相对于 plugin 仓库根目录）：

- 文件: `plugins/oinone-pamirs/skills/oinone-graphql/reference.md`

> 在 Codex CLI 中，请在本 marketplace 仓库根目录启动 codex，相对路径方可解析；或将仓库路径作为前缀传入 Read 工具。
