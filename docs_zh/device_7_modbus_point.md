# device_7_modbus_point.md - Modbus 点位服务

## 概述

Modbus 点位服务 (`IotDeviceModbusPointServiceImpl`) 负责管理 IoT 设备的 Modbus 点位配置。在 Modbus 通信中，点位代表设备上的具体数据点，如线圈状态、离散输入、保持寄存器和输入寄存器等。该服务确保每个设备的 Modbus 点位配置正确映射到物模型属性，实现设备数据的标准化访问。

## 核心职责

1. **Modbus 点位管理**
   - 创建 Modbus 点位：将物模型属性映射到设备的 Modbus 地址
   - 更新 Modbus 点位：修改点位的属性或重新映射到不同的物模型属性
   - 删除 Modbus 点位：移除不再需要的点位配置
   - 查询 Modbus 点位：根据 ID 分页查询或批量获取点位信息

2. **设备和物模型验证**
   - 确保点位所属的设备存在
   - 验证点位映射的物模型属性存在
   - 防止同一设备下存在重复的点位标识符（identifier）

3. **批量操作支持**
   - 根据设备 ID 批量获取启用的点位
   - 根据物模型 ID 批量更新点位名称和标识符
   - 支持高效的点位数据组织和访问

4. **数据一致性维护**
   - 在创建/更新点位时自动同步物模型属性的名称和标识符
   - 确保点位数据与物模型保持一致
   - 在删除点位或设备时清理相关缓存

## 关键方法

### 点位操作
- `createDeviceModbusPoint(IotDeviceModbusPointSaveReqVO createReqVO)`：创建新的 Modbus 点位
- `updateDeviceModbusPoint(IotDeviceModbusPointSaveReqVO updateReqVO)`：更新现有的 Modbus 点位
- `deleteDeviceModbusPoint(Long id)`：删除指定 ID 的 Modbus 点位
- `getDeviceModbusPoint(Long id)`：根据 ID 获取 Modbus 点位信息
- `getDeviceModbusPointPage(IotDeviceModbusPointPageReqVO pageReqVO)`：分页查询 Modbus 点位列表

### 特殊操作
- `updateDeviceModbusPointByThingModel(Long thingModelId, String identifier, String name)`：根据物模型 ID 和标识符批量更新点位名称和标识符
- `getEnabledDeviceModbusPointMapByDeviceIds(Collection<Long> deviceIds)`：根据设备 ID 列表获取启用的点位映射（设备 ID -> 点位列表）

### 验证方法
- `validateDeviceModbusPointExists(Long id)`：验证 Modbus 点位是否存在
- `validateThingModelExists(Long id)`：验证物模型是否存在
- `validateDeviceModbusPointUnique(Long deviceId, String identifier, Long excludeId)`：验证同一设备下点位标识符的唯一性

## 设计特点

1. **物模型驱动**：
   - 点位的名称和标识符自动从关联的物模型属性中同步
   - 确保点位名称和标识符与物模型定义保持一致
   - 减少手动输入错误，提高配置准确性

2. **唯一性约束**：
   - 在同一设备下，点位标识符（identifier）必须唯一
   - 防止同一设备上出现重复的 Modbus 地址映射
   - 支持排除自身 ID 进行更新时的唯一性检查

3. **级联更新**：
   - 当物模型属性名称或标识符更新时，可通过 `updateDeviceModbusPointByThingModel` 方法批量更新所有关联的点位
   - 提高物模型变更时的维护效率

4. **按设备分组查询**：
   - `getEnabledDeviceModbusPointMapByDeviceIds` 方法返回按设备分组的点位映射
   - 便于在设备通信时快速获取所需的点位配置
   - 过滤只返回启用状态的点位，提高效率

5. **缓存一致性**：
   - 在创建、更新、删除点位后调用删除设备缓存的方法
   - 确保设备相关缓存及时更新，避免脏读

## 数据模型

服务操作的核心数据对象是 `IotDeviceModbusPointDO`，它映射到数据库表 `iot_device_modbus_point`，主要字段包括：

- `id`：主键ID
- `device_id`：设备ID（外键）
- `thing_model_id`：物模型ID（外键）
- `identifier`：点位标识符（在同一设备下唯一，对应物模型属性的identifier）
- `name`：点位名称（同步自物模型属性名称）
- `address`：Modbus地址
- `quantity`：点位数量（对于连续寄存器）
- `data_type`：数据类型
- `status`：状态（启用/禁用）
- 其他扩展字段...

## 与其他服务的交互

1. **设备服务 (`IotDeviceService`)**：
   - 验证设备是否存在：`deviceService.validateDeviceExists(createReqVO.getDeviceId())`
   - 确保只能为存在的设备创建点位配置

2. **物模型服务 (`IotThingModelService`)**：
   - 验证物模型属性是否存在：`thingModelService.getThingModel(createReqVO.getThingModelId())`
   - 获取物模型属性的名称和标识符用于同步到点位
   - 批量更新点位时获取物模型信息：`thingModelService.getThingModel(thingModelId)`

3. **Modbus 点位映射器 (`IotDeviceModbusPointMapper`)**：
   - 所有数据库操作通过 MyBatis Mapper 实现
   - 包含按 ID 查询、按设备 ID 和标识符查询、插入、更新、删除和条件查询等方法
   - 特殊方法如 `selectListByDeviceIdsAndStatus` 用于批量获取启用的点位

## 性能考虑

1. **复合索引**：在 `(device_id, identifier)` 上添加唯一索引，同时支持快速查询和唯一性约束检查
2. **外键索引**：在 `device_id` 和 `thing_model_id` 上添加索引，提高关联查询性能
3. **状态索引**：在 `status` 字段上添加索引，加速按状态查询
4. **批量操作优化**：`getEnabledDeviceModbusPointMapByDeviceIds` 方法通过一次查询获取多个设备的所有点位，然后在内存中分组，减少数据库往返次数
5. **对象复用**：使用 `BeanUtils.copyProperties` 高效地复制对象属性，减少对象创建开销

## 使用示例

```java
// 创建Modbus点位
IotDeviceModbusPointSaveReqVO createReqVO = new IotDeviceModbusPointSaveReqVO();
createReqVO.setDeviceId(1L);
createReqVO.setThingModelId(10L);
// 设置其他必要属性如address、quantity、data_type等
Long pointId = deviceModbusPointService.createDeviceModbusPoint(createReqVO);

// 更新Modbus点位
IotDeviceModbusPointSaveReqVO updateReqVO = new IotDeviceModbusPointSaveReqVO();
updateReqVO.setId(pointId);
updateReqVO.setDeviceId(1L);
updateReqVO.setThingModelId(10L);
// 更新其他属性
deviceModbusPointService.updateDeviceModbusPoint(updateReqVO);

// 根据物模型批量更新点位名称和标识符
deviceModbusPointService.updateDeviceModbusPointByThingModel(10L, "new_identifier", "New Point Name");

// 获取单个点位
IotDeviceModbusPointDO point = deviceModbusPointService.getDeviceModbusPoint(pointId);

// 分页查询点位
IotDeviceModbusPointPageReqVO pageReqVO = new IotDeviceModbusPointPageReqVO();
pageReqVO.setPageNo(1);
pageReqVO.setPageSize(20);
PageResult<IotDeviceModbusPointDO> pageResult = deviceModbusPointService.getDeviceModbusPointPage(pageReqVO);

// 批量获取多个设备的启用点位
List<Long> deviceIds = Arrays.asList(1L, 2L, 3L);
Map<Long, List<IotDeviceModbusPointDO>> devicePointMap = deviceModbusPointService.getEnabledDeviceModbusPointMapByDeviceIds(deviceIds);
// 现在可以通过deviceId快速获取对应设备的所有启用点位

// 删除点位
deviceModbusPointService.deleteDeviceModbusPoint(pointId);
```

## 异常处理

服务中定义了多个业务异常情况：
1. `DEVICE_MODBUS_POINT_NOT_EXISTS`：当尝试访问不存在的Modbus点位时抛出
2. `THING_MODEL_NOT_EXISTS`：当尝试使用不存在的物模型ID时抛出
3. `DEVICE_MODBUS_POINT_EXISTS`：当尝试创建具有重复标识符的点位时抛出（同一设备下）
4. `DEVICE_NOT_EXISTS`：间接通过设备服务验证时可能抛出

这些异常通过 `ServiceExceptionUtil.exception()` 方法统一抛出，确保错误信息的一致性和可追踪性。

## 应用场景

1. **设备初始配置**：当新设备接入系统时，需要根据其物模型配置相应的Modbus点位映射
2. **物模型更新**：当物模型定义变更时，需要同步更新所有关联设备的点位名称和标识符
3. **设备数据采集**：在实际通信过程中，系统需要根据点位配置从设备读取或写入特定的Modbus地址
4. **点位使能/禁用**：通过修改点位的状态字段，可以临时禁用某些点位而不删除配置
5. **批量配置导入**：支持通过Excel等方式批量导入点位配置，服务会自动验证和同步物模型信息