# Oinone GraphQL 映射参考表

## 模型 → GraphQL Wrapper 映射

### distribution 领域（前缀 `market.*`）

| 模型类 | `@Model.model()` | Query Wrapper | Mutation Wrapper |
|---|---|---|---|
| `CommissionRecord` | `market.CommissionRecord` | `commissionRecordQuery` | `commissionRecordMutation` |
| `DistributionAccount` | `market.DistributionAccount` | `distributionAccountQuery` | `distributionAccountMutation` |
| `DistributionBinding` | `market.DistributionBinding` | `distributionBindingQuery` | `distributionBindingMutation` |
| `DistributionConfig` | `market.DistributionConfig` | `distributionConfigQuery` | `distributionConfigMutation` |
| `WithdrawalAccount` | `market.WithdrawalAccount` | `withdrawalAccountQuery` | `withdrawalAccountMutation` |
| `WithdrawalApplication` | `market.WithdrawalApplication` | `withdrawalApplicationQuery` | `withdrawalApplicationMutation` |

### order 领域（前缀 `pamirs.market.*`）

| 模型类 | `@Model.model()` | Query Wrapper | Mutation Wrapper |
|---|---|---|---|
| `MarketOrder` | `pamirs.market.MarketOrder` | `marketOrderQuery` | `marketOrderMutation` |
| `MarketDownloadHistory` | `pamirs.market.MarketDownloadHistory` | `marketDownloadHistoryQuery` | `marketDownloadHistoryMutation` |

### product 领域（前缀 `pamirs.market.*`）

| 模型类 | `@Model.model()` | Query Wrapper | Mutation Wrapper |
|---|---|---|---|
| `MarketProduct` | `pamirs.market.MarketProduct` | `marketProductQuery` | `marketProductMutation` |
| `MarketProductSnapshot` | `pamirs.market.MarketProductSnapshot` | `marketProductSnapshotQuery` | `marketProductSnapshotMutation` |
| `MarketVersionInfo` | `pamirs.market.MarketVersionInfo` | `marketVersionInfoQuery` | `marketVersionInfoMutation` |
| `MarketAbstractProduct` | `pamirs.market.MarketAbstractProduct` | `marketAbstractProductQuery` | `marketAbstractProductMutation` |

### user 领域

| 模型类 | `@Model.model()` | Query Wrapper | Mutation Wrapper |
|---|---|---|---|
| `MarketUser` | `pamirs.market.MarketUser` | `marketUserQuery` | `marketUserMutation` |

### config 领域

| 模型类 | `@Model.model()` | Query Wrapper | Mutation Wrapper |
|---|---|---|---|
| `MarketCarousel` | `pamirs.market.MarketCarousel` | `marketCarouselQuery` | `marketCarouselMutation` |
| `MarketPlatformVersion` | `pamirs.market.MarketPlatformVersion` | `marketPlatformVersionQuery` | `marketPlatformVersionMutation` |

### wish 领域

| 模型类 | `@Model.model()` | Query Wrapper | Mutation Wrapper |
|---|---|---|---|
| `MarketWishList` | `pamirs.market.MarketWishList` | `marketWishListQuery` | `marketWishListMutation` |
| `MarketLikeRecord` | `pamirs.market.MarketLikeRecord` | `marketLikeRecordQuery` | `marketLikeRecordMutation` |
| `MarketHidden` | `pamirs.market.MarketHidden` | `marketHiddenQuery` | `marketHiddenMutation` |

### wechat 领域

| 模型类 | `@Model.model()` | Query Wrapper | Mutation Wrapper |
|---|---|---|---|
| `WechatLoginUser` | `pamirs.market.wechat.WechatLoginUser` | `wechatLoginUserQuery` | `wechatLoginUserMutation` |
| `WechatRedirectRequest` | `pamirs.market.wechat.WechatRedirectRequest` | `wechatRedirectRequestQuery` | `wechatRedirectRequestMutation` |
| `WechatRedirectResponse` | `pamirs.market.wechat.WechatRedirectResponse` | `wechatRedirectResponseQuery` | `wechatRedirectResponseMutation` |

---

## Action 类 → 方法签名映射

### `DistributionAction` → `distributionBindingMutation`

绑定模型：`DistributionBinding`（`market.DistributionBinding`）

| 方法名 | 参数名 | 参数类型 | 返回类型 |
|---|---|---|---|
| `captureInviter` | `binding` | `DistributionBinding` | `DistributionBinding` |
| `bindOnRegistration` | `binding` | `DistributionBinding` | `DistributionBinding` |
| `unbindToPublicSea` | `binding` | `DistributionBinding` | `DistributionBinding` |

### `DistributionAccountAction` → `distributionAccountMutation`

绑定模型：`DistributionAccount`（`market.DistributionAccount`）

| 方法名 | 参数名 | 参数类型 | 返回类型 |
|---|---|---|---|
| `upgradeToDistributor` | `account` | `DistributionAccount` | `DistributionAccount` |
| `mapDirectToRecommender` | `account` | `DistributionAccount` | `DistributionAccount` |

### `DistributorConsoleAction` → `distributionAccountMutation`

绑定模型：`DistributionAccount`（`market.DistributionAccount`）— 与 `DistributionAccountAction` 共享同一 wrapper

| 方法名 | 参数名 | 参数类型 | 返回类型 |
|---|---|---|---|
| `viewDashboard` | `account` | `DistributionAccount` | `DistributionAccount` |
| `viewAssessmentProgress` | `account` | `DistributionAccount` | `DistributionAccount` |
| `generatePromotionLink` | `account` | `DistributionAccount` | `DistributionAccount` |
| `generatePoster` | `account` | `DistributionAccount` | `DistributionAccount` |

### `AdminRiskAction` → `distributionAccountMutation`

绑定模型：`DistributionAccount`（`market.DistributionAccount`）— 与上述两个 Action 共享同一 wrapper

| 方法名 | 参数名 | 参数类型 | 返回类型 |
|---|---|---|---|
| `freezeAccount` | `account` | `DistributionAccount` | `DistributionAccount` |
| `unfreezeAccount` | `account` | `DistributionAccount` | `DistributionAccount` |
| `clearBindingRelations` | `account` | `DistributionAccount` | `DistributionAccount` |
| `deductUnsettledCommissions` | `account` | `DistributionAccount` | `DistributionAccount` |

### `AdminFinanceAction` → `withdrawalApplicationMutation`

绑定模型：`WithdrawalApplication`（`market.WithdrawalApplication`）

| 方法名 | 参数名 | 参数类型 | 返回类型 |
|---|---|---|---|
| `submitWithdrawal` | `application` | `WithdrawalApplication` | `WithdrawalApplication` |
| `approveWithdrawal` | `application` | `WithdrawalApplication` | `WithdrawalApplication` |
| `rejectWithdrawal` | `application` | `WithdrawalApplication` | `WithdrawalApplication` |
| `confirmPayment` | `application` | `WithdrawalApplication` | `WithdrawalApplication` |
| `exportReport` | `application` | `WithdrawalApplication` | `WithdrawalApplication` |

### `CheckoutAction` → `marketOrderMutation`

绑定模型：`MarketOrder`（`pamirs.market.MarketOrder`）

| 方法名 | 参数名 | 参数类型 | 返回类型 |
|---|---|---|---|
| `initiateCheckout` | `order` | `MarketOrder` | `MarketOrder` |
| `cancelExpiredOrder` | `order` | `MarketOrder` | `MarketOrder` |
| `applyRefund` | `order` | `MarketOrder` | `MarketOrder` |
| `confirmRefund` | `order` | `MarketOrder` | `MarketOrder` |

### `MarketUserAction` → `marketUserQuery`（`@Function` 方法）

绑定模型：`MarketUser`（`pamirs.market.MarketUser`）

| 方法名 | 参数1 | 参数2 | 返回类型 |
|---|---|---|---|
| `queryPageForCusOrderSearch` | `page: Pagination<MarketUser>` | `queryWrapper: IWrapper<MarketUser>` | `Pagination<MarketUser>` |
| `queryPageForCusDhSearch` | `page: Pagination<MarketUser>` | `queryWrapper: IWrapper<MarketUser>` | `Pagination<MarketUser>` |
| `queryPageForDevOrderSearch` | `page: Pagination<MarketUser>` | `queryWrapper: IWrapper<MarketUser>` | `Pagination<MarketUser>` |
| `queryPageForAdmProductAndSnapshotSearch` | `page: Pagination<MarketUser>` | `queryWrapper: IWrapper<MarketUser>` | `Pagination<MarketUser>` |
| `queryPageForAdmWishSearch` | `page: Pagination<MarketUser>` | `queryWrapper: IWrapper<MarketUser>` | `Pagination<MarketUser>` |
| `queryPageForAdmOrderDeveloperSearch` | `page: Pagination<MarketUser>` | `queryWrapper: IWrapper<MarketUser>` | `Pagination<MarketUser>` |
| `queryPageForAdmOrderBuyerSearch` | `page: Pagination<MarketUser>` | `queryWrapper: IWrapper<MarketUser>` | `Pagination<MarketUser>` |

### `UserWechatAction` → 绑定 `PamirsUserTransient`（`@Function` 方法）

| 方法名 | 参数名 | 参数类型 | 返回类型 |
|---|---|---|---|
| `fetchWechatRedirectUrl` | `request` | `WechatRedirectRequest` | `WechatRedirectResponse` |
| `loginByWechat` | `code` | `String` | `WechatLoginUser` |
| `bindWechat` | `code` | `String` | `MarketUser` |

---

## GraphQL 示例速查

### @Action 方法调用

```graphql
# captureInviter（DistributionAction → distributionBindingMutation）
mutation {
  distributionBindingMutation {
    captureInviter(binding: {
      customerUserId: 10086
      distributorId: 20001
      lastClickSource: "LINK"
      channel: "wechat_group"
    }) {
      id
      customerUserId
      distributorId
      lastClickAt
    }
  }
}
```

### 默认分页查询（queryPage）

```graphql
# CommissionRecord 分页查询
query {
  commissionRecordQuery {
    queryPage(page: { currentPage: 1, size: 20 }, queryWrapper: { rsql: "distributorId==20001;status==FROZEN" }) {
      content {
        id
        orderId
        amount
        status
      }
      totalElements
      totalPages
    }
  }
}
```

### 单条查询（queryOne）

```graphql
query {
  distributionAccountQuery {
    queryOne(queryWrapper: { rsql: "id==20001" }) {
      id
      userId
      inviteCode
      status
    }
  }
}
```