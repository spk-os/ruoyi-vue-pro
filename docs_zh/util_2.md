# TenantUtils 多租户工具类

## 概述
`TenantUtils` 是一个用于在多租户系统中管理租户上下文的工具类。它提供了一系列静态方法，用于在执行特定逻辑时临时切换租户 ID 或忽略租户检查，并在执行完成后恢复原始状态。此外，它还提供了将租户 ID 添加到 HTTP 请求头的工具方法。

## 核心功能
1. **临时租户切换**：在执行给定的 `Runnable` 或 `Callable` 时，临时将当前租户 ID 设置为指定值，并在执行后恢复原始租户 ID。
2. **忽略租户检查**：在执行给定的 `Runnable` 或 `Callable` 时，临时设置为忽略租户检查（即不进行租户隔离），并在执行后恢复原始设置。
3. **租户 ID 传播**：将租户 ID 添加到 HTTP 请求头中，以便在微服务间传播租户信息。

## 关键方法

### `execute(Long tenantId, Runnable runnable)`
- **描述**：在指定租户 ID 下执行给定的无返回值逻辑。
- **参数**：
  - `tenantId`: 要切换到的租户 ID。
  - `runnable`: 要执行的逻辑。
- **注意**：
  - 如果当前上下文是忽略租户的状态，该方法会强制设置为不忽略租户（即开启租户隔离），执行后恢复原始忽略状态。
  - 该方法会保存当前租户 ID 和忽略状态，执行逻辑后恢复。

### `execute(Long tenantId, Callable<V> callable)`
- **描述**：在指定租户 ID 下执行给定的有返回值逻辑。
- **参数**：
  - `tenantId`: 要切换到的租户 ID。
  - `callable`: 要执行的逻辑，返回值类型为 `V`。
- **返回值**：`callable` 的执行结果。
- **注意**：同上，会临时切换租时切换租户 ID 和忽略状态，并在执行后恢复。

### `executeIgnore(Runnable runnable)`
- **描述**：在忽略租户检查的状态下执行给定的无返回值逻辑。
- **参数**：
  - `runnable`: 要执行的逻辑。
- **注意**：
  - 该方法会保存当前忽略状态，设置为忽略租户（即不进行租户隔离），执行后恢复原始忽略状态。
  - 租户 ID 在此过程中保持不变（但租户检查被忽略）。

### `executeIgnore(Callable<V> callable)`
- **描述**：在忽略租户检查的状态下执行给定的有返回值逻辑。
- **参数**：
  - `callable`: 要执行的逻辑，返回值类型为 `V`。
- **返回值**：`callable` 的执行结果。
- **注意**：同上。

### `addTenantHeader(Map<String, String> headers, Long tenantId)`
- **描述**：将租户 ID 添加到 HTTP 请求头中。
- **参数**：
  - `headers`: HTTP 请求头的映射。
  - `tenantId`: 要添加的租户 ID（如果为 null，则不添加）。
- **实现**：使用 `WebFrameworkUtils.HEADER_TENANT_ID` 作为头的键，将租户 ID 转换为字符串作为值。

## 与其他模块的关系
- `TenantUtils` 依赖于 `TenantContextHolder`（来自同一模块的 `cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder`）来管理租户上下文。
- 它使用 `WebFrameworkUtils.HEADER_TENANT_ID`（来自 `cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils`）来定义租户 ID 在 HTTP 头中的键。
- 在微服务架构中，`TenantUtils` 的 `addTenantHeader` 方法常用于在服务间调用时传播租户 ID，确保下游服务能够正确识别并处理租户数据。

## 使用示例
```java
// 示例1：在租户 ID 为 1001 的情况下执行一些逻辑
TenantUtils.execute(1001L, () -> {
    // 这里的操作会在租户 1001 的上下文中执行
    // 例如：查询数据时会自动加上租户条件
});

// 示例2：忽略租户检查，执行一些需要跨租户的操作（如管理员操作）
TenantUtils.executeIgnore(() -> {
    // 这里的操作会忽略租户隔离，可以访问所有租户的数据
});

// 示例3：准备发送HTTP请求时，添加租户ID到头部
Map<String, String> headers = new HashMap<>();
TenantUtils.addTenantHeader(headers, 1001L);
// 然后使用 headers 发送请求
```

## 注意事项
- 所有执行方法（`execute` 和 `executeIgnore`）都使用 `try-finally` 块来确保上下文状态在执行后被正确恢复，即使在执行过程中发生异常。
- 因此，这些方法是线程安全的，因为它们使用了 `ThreadLocal` 来存储租户上下文（通过 `TenantContextHolder`）。

## 相关文档
- [TenantContextHolder](context.md)：详细说明租户上下文持有者的实现。
- [WebFrameworkUtils](utils.md)：详细说明 Web 框架工具类。

## 结语
`TenantUtils` 是多租户系统中一个简单但关键的工具类，它通过临时修改租户上下文来实现租户隔离的灵活控制，确保在不同业务场景下能够正确地处理租户数据。