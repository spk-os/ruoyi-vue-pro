# device_7_group.md - 设备分组服务

## 概述

设备分组服务 (`IotDeviceGroupServiceImpl`) 负责 IoT 设备分组的管理，提供设备分组的创建、更新、删除、查询等功能。设备分组是对设备进行逻辑分组的机制，便于设备的批量管理和操作。

## 核心职责

1. **设备分组生命周期管理**
   - 创建设备分组：接收分组信息并持久化到数据库
   - 更新设备分组：修改已有分组的属性
   - 删除设备分组：在删除前检查是否有设备关联，防止误删
   - 查询设备分组：支持单个查询、分页查询、条件查询等

2. **设备分组关联管理**
   - 检查设备分组是否被设备引用：在删除分组前验证是否有设备使用该分组
   - 根据状态查询设备分组：支持按状态（启用/禁用）查询分组列表

3. **设备分组查询优化**
   - 支持按名称精确查询设备分组
   - 提供分页查询功能，适用于大数据量场景
   - 支持按状态批量查询设备分组

## 关键方法

### 分组操作
- `createDeviceGroup(IotDeviceGroupSaveReqVO createReqVO)`：创建新设备分组
- `updateDeviceGroup(IotDeviceGroupSaveReqVO updateReqVO)`：更新设备分组信息
- `deleteDeviceGroup(Long id)`：删除指定ID的设备分组
- `getDeviceGroup(Long id)`：根据ID获取设备分组信息
- `getDeviceGroupByName(String name)`：根据名称获取设备分组
- `getDeviceGroupPage(IotDeviceGroupPageReqVO pageReqVO)`：分页查询设备分组列表
- `getDeviceGroupListByStatus(Integer status)`：根据状态获取设备分组列表

### 验证方法
- `validateDeviceGroupExists(Long id)`：验证设备分组是否存在，不存在则抛出异常

## 设计特点

1. **简单直接的CRUD操作**：所有方法都围绕设备分组的基本增删改查操作展开
2. **前置验证机制**：在执行更新和删除操作前，先验证目标对象是否存在
3. **业务规则 enforcement**：在删除操作前检查是否有设备关联，防止误删导致数据不一致
4. **状态管理**：支持按状态查询分组，便于实现分组的启用/禁用功能
5. **名称唯一性**：通过数据库唯一约束或业务逻辑确保分组名称在系统中的唯一性（虽然在当前实现中未显示体现，但通常需要）

## 与其他服务的交互

1. **设备服务 (`IotDeviceService`)**：
   - 在删除设备分组前，调用设备服务检查是否有设备使用该分组
   - `deviceService.getDeviceCountByGroupId(id) > 0` 用于判断分组是否被引用

2. **设备分组映射器 (`IotDeviceGroupMapper`)**：
   - 所有数据库操作都通过 MyBatis Mapper 实现
   - 包含增删改查以及特殊查询（如按名称查询、按状态查询等）

## 性能考虑

1. **索引优化**：建议在 `name` 和 `status` 字段上创建数据库索引，以支持快速查询
2. **前置验证减少无效操作**：在更新和删除前先验证记录是否存在，避免不必要的数据库操作
3. **批量操作支持**：虽然当前接口主要是单个操作，但可以通过循环调用实现批量处理

## 使用示例

```java
// 创建设备分组
IotDeviceGroupSaveReqVO createReqVO = new IotDeviceGroupSaveReqVO();
createReqVO.setName("传感器设备组");
createReqVO.setStatus(1); // 启用状态
Long groupId = deviceGroupService.createDeviceGroup(createReqVO);

// 查询设备分组
IotDeviceGroupDO group = deviceGroupService.getDeviceGroup(groupId);

// 更新设备分组
IotDeviceGroupSaveReqVO updateReqVO = new IotDeviceGroupSaveReqVO();
updateReqVO.setId(groupId);
updateReqVO.setName("更新后的设备组名称");
updateReqVO.setStatus(0); // 禁用状态
deviceGroupService.updateDeviceGroup(updateReqVO);

// 分页查询设备分组
IotDeviceGroupPageReqVO pageReqVO = new IotDeviceGroupPageReqVO();
pageReqVO.setPageNo(1);
pageReqVO.setPageSize(10);
PageResult<IotDeviceGroupDO> pageResult = deviceGroupService.getDeviceGroupPage(pageReqVO);

// 按状态查询设备分组
List<IotDeviceGroupDO> activeGroups = deviceGroupService.getDeviceGroupListByStatus(1);

// 删除设备分组（会先检查是否有设备使用该分组）
deviceGroupService.deleteDeviceGroup(groupId);
```

## 异常处理

服务中定义了两个主要的业务异常：
1. `DEVICE_GROUP_NOT_EXISTS`：当尝试访问不存在的设备分组时抛出
2. `DEVICE_GROUP_DELETE_FAIL_DEVICE_EXISTS`：当尝试删除仍有设备关联的设备分组时抛出

这些异常通过 `ServiceExceptionUtil.exception()` 方法统一抛出，确保错误信息的一致性和可追踪性。