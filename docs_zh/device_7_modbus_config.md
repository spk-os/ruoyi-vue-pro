# device_7_modbus_config.md - Modbus 配置服务

## 概述

Modbus 配置服务 (`IotDeviceModbusConfigServiceImpl`) 负责管理 IoT 设备的 Modbus 通信配置。Modbus 是工业领域广泛使用的通信协议，该服务确保设备能够根据不同的 Modbus 模式（主站/从站、TCP 服务器/客户端）正确配置通信参数。

## 核心职责

1. **Modbus 配置管理**
   - 保存 Modbus 配置：根据设备 ID 创建或更新 Modbus 配置记录
   - 查询 Modbus 配置：根据 ID 或设备 ID 获取特定的 Modbus 配置
   - 列表查询：支持根据条件查询多个 Modbus 配置记录

2. **协议类型验证**
   - 根据产品的协议类型验证 Modbus 配置的有效性
   - 不同的 Modbus 协议类型需要不同的必填字段：
     * MODBUS_TCP_CLIENT 模式：需要 IP 地址、端口、超时时间、重试间隔
     * MODBUS_TCP_SERVER 模式：需要工作模式、数据帧格式

3. **数据一致性保障**
   - 在保存配置前验证设备是否存在
   - 在保存配置前验证产品是否存在且与设备匹配
   - 确保每个设备只能有一个 Modbus 配置记录（通过设备ID作为唯一键）

## 关键方法

### 配置操作
- `saveDeviceModbusConfig(IotDeviceModbusConfigSaveReqVO saveReqVO)`：保存或更新设备的 Modbus 配置
- `getDeviceModbusConfig(Long id)`：根据配置 ID 获取 Modbus 配置
- `getDeviceModbusConfigByDeviceId(Long deviceId)`：根据设备 ID 获取对应的 Modbus 配置
- `getDeviceModbusConfigList(IotModbusDeviceConfigListReqDTO listReqDTO)`：根据条件查询 Modbus 配置列表

### 验证方法
- `validateModbusConfigByProtocolType(IotDeviceModbusConfigSaveReqVO saveReqVO, String protocolType)`：根据协议类型验证 Modbus 配置的必填字段

## 设计特点

1. **幂等性设计**：
   - 保存操作会先检查数据库中是否已存在该设备的 Modbus 配置
   - 如果存在则执行更新操作，如果不存在则执行插入操作
   - 这样确保了多次调用保存方法的结果是一致的

2. **协议特定验证**：
   - 根据产品的协议类型动态验证所需的字段
   - 不同的 Modbus 模式有不同的必填要求，确保配置的有效性
   - 使用枚举类 `IotProtocolTypeEnum` 来表示不同的协议类型

3. **数据一致性**：
   - 在保存前验证设备和产品的存在性
   - 确保配置与实际设备和产品匹配
   - 防止为不存在的设备创建配置

4. **解耦设计**：
   - 服务只负责 Modbus 配置的管理，不涉及具体的通信实现
   - 通过依赖注入获取所需的其他服务（设备服务、产品服务）
   - 易于单元测试和维护

## 数据模型

服务操作的核心数据对象是 `IotDeviceModbusConfigDO`，它映射到数据库表 `iot_device_modbus_config`，主要字段包括：

- `id`：主键ID
- `device_id`：设备ID（外键，唯一）
- `ip`：IP地址（客户端模式必填）
- `port`：端口号（客户端模式必填）
- `timeout`：连接超时时间（客户端模式必填）
- `retry_interval`：重试间隔（客户端模式必填）
- `mode`：工作模式（服务器模式必填）
- `frame_format`：数据帧格式（服务器模式必填）
- 其他扩展字段...

## 与其他服务的交互

1. **设备服务 (`IotDeviceService`)**：
   - 验证设备是否存在：`deviceService.validateDeviceExists(saveReqVO.getDeviceId())`
   - 确保只能为存在的设备配置 Modbus 参数

2. **产品服务 (`IotProductService`)**：
   - 获取产品信息：`productService.getProduct(device.getProductId())`
   - 根据产品的协议类型进行验证：`product.getProtocolType()`

3. **Modbus 配置映射器 (`IotDeviceModbusConfigMapper`)**：
   - 所有数据库操都通过 MyBatis Mapper 实现
   - 包含按 ID 查询、按设备 ID 查询、插入、更新和条件查询等方法

## 性能考虑

1. **唯一约束**：在 `device_id` 字段上添加唯一索引，确保每个设备只能有一个 Modbus 配置，同时提高查询性能
2. **即时验证**：在保存前进行所有必要的验证，避免无效数据写入数据库
3. **最小化数据库操作**：通过先查询再决定是插入还是更新，减少不必要的删除+插入操作

## 使用示例

```java
// 保存Modbus TCP客户端配置
IotDeviceModbusConfigSaveReqVO saveReqVO = new IotDeviceModbusConfigSaveReqVO();
saveReqVO.setDeviceId(1L);
saveReqVO.setIp("192.168.1.100");
saveReqVO.setPort(502);
saveReqVO.setTimeout(3000); // 3秒超时
saveReqVO.setRetryInterval(5000); // 5秒重试间隔
// 注意：对于客户端模式，mode和frameFormat可以为空
deviceModbusConfigService.saveDeviceModbusConfig(saveReqVO);

// 保存Modbus TCP服务器配置
IotDeviceModbusConfigSaveReqVO saveReqVO2 = new IotDeviceModbusConfigSaveReqVO();
saveReqVO2.setDeviceId(2L);
saveReqVO2.setMode(0); // 工作模式
saveReqVO2.setFrameFormat(1); // 数据帧格式
// 注意：对于服务器模式，IP、端口等可以为空
deviceModbusConfigService.saveDeviceModbusConfig(saveReqVO2);

// 根据设备ID获取Modbus配置
IotDeviceModbusConfigDO config = deviceModbusConfigService.getDeviceModbusConfigByDeviceId(1L);

// 根据配置ID获取Modbus配置
IotDeviceModbusConfigDO configById = deviceModbusConfigService.getDeviceModbusConfig(1L);

// 查询Modbus配置列表
IotModbusDeviceConfigListReqDTO listReqDTO = new IotModbusDeviceConfigListReqDTO();
// 可以设置查询条件，例如设备ID范围等
List<IotDeviceModbusConfigDO> configList = deviceModbusConfigService.getDeviceModbusConfigList(listReqDTO);
```

## 异常处理

服务中可能抛出的异常主要包括：
1. 设备不存在异常：通过 `deviceService.validateDeviceExists()` 方法抛出
2. 产品不存在异常：通过 `productService.getProduct()` 方法间接导致的空指针检查
3. 断言失败异常：在 `validateModbusConfigByProtocolType` 方法中使用 `Assert.isTrue()` 和 `Assert.notNull()` 进行验证，验证失败会抛出 `IllegalArgumentException`

这些异常将向上传播到调用层，由上层服务或控制器进行统一处理。