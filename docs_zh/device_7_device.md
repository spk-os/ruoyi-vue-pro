# device_7_device.md - 设备管理服务

## 概述

设备管理服务 (`IotDeviceServiceImpl`) 是 device_7 模块的核心服务，负责 IoT 设备的完整生命周期管理，包括设备的创建、更新、删除、查询、状态管理以及网关-子设备关系处理。

## 核心职责

1. **设备生命周期管理**
   - 设备创建：验证产品存在、设备名称唯一性、网关设备合法性、设备序列号唯一性等
   - 设备更新：修改设备属性，不允许修改设备名称和产品ID
   - 设备删除：检查网关设备是否有子设备绑定，防止误删
   - 设备查询：支持分页查询、按条件查询、按状态查询等

2. **设备状态管理**
   - 设备状态转换：未激活、在线、离线等状态的切换
   - 在线/离线时间自动记录
   - 网关下线时联动子设备下线

3. **网关-子设备关系管理**
   - 网关设备验证：确保只有网关类型设备可以有子设备
   - 子设备绑定：将子设备绑定到网关设备
   - 子设备解绑：从网关设备解绑子设备
   - 拓扑变更通知：当网关-子设备关系变化时，发送拓扑变更通知给网关设备

4. **设备动态注册**
   - 设备自动注册：根据产品密钥和设备名称自动创建设备
   - 子设备动态注册：网关设备上报子设备信息时自动注册子设备
   - 安全验证：通过产品密钥验证设备注册请求的合法性

5. **设备位置管理**
   - 设备经纬度更新：记录和更新设备的地理位置信息
   - 位置查询：获取已设置位置的设备列表

## 关键方法

### 设备操作
- `createDevice(IotDeviceSaveReqVO createReqVO)`：创建新设备
- `updateDevice(IotDeviceSaveReqVO updateReqVO)`：更新设备信息
- `deleteDevice(Long id)`：删除指定ID的设备
- `getDevice(Long id)`：根据ID获取设备信息
- `getDevicePage(IotDevicePageReqVO pageReqVO)`：分页查询设备列表

### 状态管理
- `updateDeviceState(IotDeviceDO device, Integer state)`：更新设备状态
- `updateDeviceState(Long id, Integer state)`：根据ID更新设备状态
- `getDeviceCountMapByState()`：按状态统计设备数量

### 网关-子设备关系
- `bindDeviceGateway(Collection<Long> subIds, Long gatewayId)`：绑定子设备到网关
- `unbindDeviceGateway(Collection<Long> subIds, Long gatewayId)`：解绑子设备与网关
- `getDeviceListByGatewayId(Long gatewayId)`：获取网关下的所有子设备
- `getUnboundSubDevicePage(IotDevicePageReqVO pageReqVO)`：获取未绑定网关的子设备

### 动态注册
- `registerDevice(IotDeviceRegisterReqDTO reqDTO)`：注册设备
- `registerSubDevices(IotSubDeviceRegisterFullReqDTO reqDTO)`：注册子设备
- `authDevice(IotDeviceAuthReqDTO authReqDTO)`：认证设备

### 位置管理
- `updateDeviceLocation(IotDeviceDO device, BigDecimal longitude, BigDecimal latitude)`：更新设备位置
- `getDeviceListByHasLocation()`：获取已设置位置的设备列表

## 设计特点

1. **缓存机制**：使用 Redis 缓存设备信息，提高查询性能
   - `@Cacheable` 注解用于设备查询方法
   - `@CacheEvict` 注解用于设备更新/删除时清除缓存

2. **租户隔离**：通过 `@TenantIgnore` 注解处理跨租户场景
   - 设备通过 productKey + deviceName 进行跨租户唯一标识
   - 某些操作忽略租户信息以支持跨租户场景

3. **事务管理**：关键操作使用 `@Transactional` 确保数据一致性
   - 设备导入、绑定/解绑网关等操作

4. **参数验证**：使用 Hibernate Validator 进行参数校验
   - `@Validated` 注解在类级别
   - 自定义验证方法如 `validateDeviceExists`、`validateSerialNumberUnique` 等

5. **异常处理**：统一的异常处理机制
   - 使用 `ServiceExceptionUtil.exception()` 抛出业务异常
   - 定义了丰富的错误码常量在 `ErrorCodeConstants` 中

## 与其他服务的交互

1. **产品服务 (`IotProductService`)**：
   - 获取产品信息验证设备创建/更新时的产品是否存在
   - 验证产品的动态注册是否启用
   - 获取产品的设备类型等信息

2. **设备组服务 (`IotDeviceGroupService`)**：
   - 验证设备分组是否存在
   - 批量验证多个设备分组的存在性

3. **设备消息服务 (`IotDeviceMessageService`)**：
   - 发送网关拓扑变更通知给网关设备
   - 处理设备上报的拓扑变更消息

4. **物模型服务 (`IotThingModelService`)**：
   - 在 Modbus 点位服务中使用（见 device_7_modbus_point.md）
   - 验证物模型属性是否存在

## 性能考虑

1. **缓存使用**：频繁查询的设备信息使用 Redis 缓存，减少数据库访问
2. **批量操作**：支持批量设备导入、绑定/解绑操作，减少数据库交互次数
3. **延迟加载**：使用 `@Lazy` 注解解决服务间的循环依赖问题
4. **租户隔离优化**：通过 `TenantUtils.executeIgnore()` 和 `TenantUtils.execute()` 处理租户上下文

## 使用示例

```java
// 创建设备
IotDeviceSaveReqVO createReqVO = new IotDeviceSaveReqVO();
createReqVO.setDeviceName("TemperatureSensor001");
createReqVO.setProductId(1L);
createReqVO.setGatewayId(0L); // 非网关设备
createReqVO.setGroupIds(Arrays.asList(1L, 2L));
Long deviceId = deviceService.createDevice(createReqVO);

// 更新设备状态
deviceService.updateDeviceState(deviceId, IotDeviceStateEnum.ONLINE.getState());

// 绑定子设备到网关
deviceService.bindDeviceGateway(Arrays.asList(deviceId), gatewayDeviceId);

// 注册设备
IotDeviceRegisterReqDTO registerReqDTO = new IotDeviceRegisterReqDTO();
registerReqDTO.setProductKey("productKey");
registerReqDTO.setDeviceName("deviceName");
registerReqDTO.setSign("signature");
IotDeviceRegisterRespDTO respDTO = deviceService.registerDevice(registerReqDTO);
```