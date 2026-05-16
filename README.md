# Oinone Skills Marketplace

Oinone/Pamirs aPaaS 框架研发技能合集。

## 技能清单

| skill                               | 说明                                                                               |
|-------------------------------------|----------------------------------------------------------------------------------|
| `oinone-auth-extension`             | RBAC 权限扩展：白名单、AuthFilter、数据权限、字段权限、SSO、登录流程、密码策略等                                |
| `oinone-create-action`              | `@Action` / `@UxRouteButton` / `@UxLinkButton` / `@UxClientButton` / `@Function` |
| `oinone-create-async-task`          | `@XAsync` 与 `ExecuteTaskAction` 异步任务                                             |
| `oinone-create-eip-integration`     | `@Integrate` 出站第三方接口集成（JSON / XML）                                               |
| `oinone-create-enum`                | `@Dict` + `IEnum<T>` 枚举与数据字典                                                     |
| `oinone-create-excel-import-export` | `ExcelTemplateInit` + 导入/导出 ExtPoint                                             |
| `oinone-create-hook`                | `HookBefore` / `HookAfter` 函数拦截器                                                 |
| `oinone-create-model`               | `@Model` + `@Field` 领域模型与字段                                                      |
| `oinone-create-open-api`            | `@Open` 入站开放接口                                                                   |
| `oinone-create-schedule-task`       | `ScheduleAction` 定时任务（5.x，非 cron）                                                |
| `oinone-create-trigger-task`        | `@Trigger` binlog 触发任务                                                           |
| `oinone-graphql`                    | Oinone 嵌套式 GraphQL 查询/变更生成                                                       |
| `oinone-migrate-xml`                | 数据库 XML 视图迁移到代码                                                                  |
| `oinone-simplify-xml`               | 界面设计器导出 XML 的安全精简                                                                |

## 在 Claude Code 中安装

```
/plugin marketplace add https://github.com/monkeyz6/oinone-skill.git
/plugin install oinone-pamirs@oinone-skills-marketplace
```

## 在 Codex CLI 中安装

```bash
curl -fsSL https://raw.githubusercontent.com/monkeyz6/oinone-skill/main/bootstrap-codex.sh | bash
```

卸载：

```bash
curl -fsSL https://raw.githubusercontent.com/monkeyz6/oinone-skill/main/bootstrap-codex.sh | bash -s -- --uninstall
```
