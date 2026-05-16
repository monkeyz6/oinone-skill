# 导入运行时

## eachImport 语义

| 值       | 泛型             | importData 入参              | 适用                              |
|---------|----------------|----------------------------|---------------------------------|
| `true`  | `<Model>`      | `(context, Model data)`    | 默认推荐。每行一次回调，错误隔离。               |
| `false` | `<List<Model>>`| `(context, List<Model>)`   | 需要整体校验、跨行去重、批量保存时               |

## 行号偏移

| 模板类型             | 表头行数 | 数据起始行 | 错误行号公式            |
|------------------|------|-------|-------------------|
| 简单（默认）           | 2    | 第 3 行 | `index + 3`       |
| 复杂（含配置/说明/显示三行） | 3    | 第 4 行 | `index + 4`       |
| 自定义表头数           | N    | 第 N+1 行 | `index + N + 1` |

> 简单模板：第 1 行 = 配置行（隐藏），第 2 行 = 显示表头。

## 错误消息回写

```java
ExcelImportTask importTask = importContext.getImportTask();

// 添加错误（不中断导入）
importTask.addTaskMessage(TaskMessageLevelEnum.ERROR, "第3行: 编码不能为空");

// 添加警告
importTask.addTaskMessage(TaskMessageLevelEnum.WARN, "...");
```

`TaskMessageLevelEnum`：`ERROR` / `WARN` / `INFO`。

## importData 返回值

- 返回 `Boolean.TRUE`：该行/该批继续被框架处理（创建任务结果）
- 返回 `Boolean.FALSE`：跳过该行/该批

> **`@ExtPoint.Implement` 必须返回 `Boolean` 而不是基本类型 `boolean`**。

## 错误处理范式

```java
@Override
public Boolean importData(ExcelImportContext importContext, Model data) {
    ExcelImportTask importTask = importContext.getImportTask();
    try {
        checkRequired(data);     // 必填校验
        validateBusiness(data);  // 业务校验（查重、关联校验）
        saveData(data);          // 持久化
    } catch (PamirsException e) {
        log.error("导入业务异常", e);
        importTask.addTaskMessage(TaskMessageLevelEnum.ERROR, e.getMessage());
    } catch (Exception e) {
        log.error("导入异常", e);
        importTask.addTaskMessage(TaskMessageLevelEnum.ERROR, "导入失败: " + e.getMessage());
    }
    return Boolean.TRUE;
}
```

## 必填校验

不要用 `appendMsg(...)` 直接拼字符串绕过错误码。**优先用 `ExpEnumerate` 错误码枚举**：

```java
if (StringUtils.isEmpty(data.getCode())) {
    throw PamirsException.construct(ExpEnumerate.BIZ_ERROR)
            .appendMsg("必填字段_编码_没填").errThrow();
}
```

## 批量导入特殊处理

```java
@Override
public Boolean importData(ExcelImportContext importContext, List<Model> dataList) {
    ExcelImportTask importTask = importContext.getImportTask();
    if (CollectionUtils.isEmpty(dataList)) {
        importTask.addTaskMessage(TaskMessageLevelEnum.ERROR, "导入内容不能为空");
        return Boolean.FALSE;
    }

    boolean valid = true;
    for (int i = 0; i < dataList.size(); i++) {
        Model data = dataList.get(i);
        if (StringUtils.isEmpty(data.getCode())) {
            importTask.addTaskMessage(TaskMessageLevelEnum.ERROR,
                    "第" + (i + 3) + "行: 编码不能为空");
            valid = false;
        }
    }
    if (!valid) return Boolean.FALSE;

    // 批量保存
    Models.data().createOrUpdateBatch(dataList);
    return Boolean.TRUE;
}
```

注意点：

- 批量模式失败时整批回滚（如果开启 `hasErrorRollback`）。
- 行号计算要带上索引（如 `i + 3`），否则用户看不出错误在第几行。
- 跨行唯一性校验只能在批量模式下做。
