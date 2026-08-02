# OTA 固件管理模块 (ota_4_firmware_management)

## 模块概述

OTA 固件管理模块负责物联网设备固件的上传、版本管理和分发。它提供固件的创建、更新、查询等功能，并自动计算固件文件的MD5签名以确保文件完整性。该模块是物联网平台OTA（Over-The-Air）升级功能的核心组件。

## 核心功能

1. **固件管理**：支持固件的创建、更新、删除和查询操作
2. **版本控制**：确保同一产品下固件版本号的唯一性
3. **文件完整性验证**：自动下载固件文件并计算MD5签名
4. **产品关联**：每个固件记录关联到具体的物联网产品
5. **分页查询**：支持固件列表的分页查询和过滤

## 架构设计

### 模块定位

OTA 固件管理模块位于物联网平台的服务层，负责处理固件相关的业务逻辑。它依赖于数据访问层（Mapper）进行数据库操作，并调用产品服务进行产品验证。

### 核心组件关系

```
graph TD
    A[IotOtaFirmwareServiceImpl] --> B[IotOtaFirmwareMapper]
    A --> C[IotProductService]
    B --> D[(IotOtaFirmware表)]
    C --> E[产品服务]
    F[控制器层] --> A
    G[其他业务服务] --> A
```

### 依赖关系

```
graph LR
    A[IotOtaFirmwareServiceImpl] -->|依赖| B[IotOtaFirmwareMapper]
    A -->|依赖| C[IotProductService]
    A -->|使用| D[BeanUtils]
    A -->|使用| E[Hutool HTTP]
    A -->|使用| F[Hutool Digest]
    A -->|使用| G[Lombok Slf4j]
    H[Spring框架] --> A
```

## 数据流说明

### 固件创建流程

```
sequenceDiagram
    participant 控制器 as OTA固件控制器
    participant 服务 as IotOtaFirmwareServiceImpl
    participant Mapper as IotOtaFirmwareMapper
    participant 产品服务 as IotProductService
    participant HTTP as HTTP客户端
    
    控制器->>服务: createOtaFirmware(固件创建请求)
    服务->>产品服务: validateProductExists(产品ID)
    产品服务-->>服务: 验证结果
    服务->>Mapper: selectByProductIdAndVersion(产品ID, 版本)
    Mapper-->>服务: 现有固件记录(或null)
    alt 版本重复
        服务-->>控制器: 抛出版本重复异常
    else 版本唯一
        服务->>服务: BeanUtils转换为DO对象
        服务->>HTTP: downloadBytes(文件URL)
        HTTP-->>服务: 文件字节数组
        服务->>服务: 计算文件大小和MD5签名
        服务->>Mapper: insert(固件DO对象)
        Mapper-->>服务: 生成的固件ID
        服务-->>控制器: 固件ID
    end
```

### 固件查询流程

```
sequenceDiagram
    participant 控制器 as OTA固件控制器
    participant 服务 as IotOtaFirmwareServiceImpl
    participant Mapper as IotOtaFirmwareMapper
    
    控制器->>服务: getOtaFirmwarePage(分页请求)
    服务->>Mapper: selectPage(分页请求)
    Mapper-->>服务: 分页结果
    服务-->>控制器: 分页结果
```

## 详细设计

### 类结构

```
classDiagram
    class IotOtaFirmwareServiceImpl {
        -IotOtaFirmwareMapper otaFirmwareMapper
        -IotProductService productService
        +Long createOtaFirmware(IotOtaFirmwareCreateReqVO)
        +void updateOtaFirmware(IotOtaFirmwareUpdateReqVO)
        +IotOtaFirmwareDO getOtaFirmware(Long)
        +IotOtaFirmwareDO getOtaFirmwareByProductIdAndVersion(Long, String)
        +List<IotOtaFirmwareDO> getOtaFirmwareList(Collection<Long>)
        +PageResult<IotOtaFirmwareDO> getOtaFirmwarePage(IotOtaFirmwarePageReqVO)
        +IotOtaFirmwareDO validateFirmwareExists(Long)
        -void calculateFileDigest(IotOtaFirmwareDO)
    }
    
    IotOtaFirmwareMapper <|.. IotOtaFirmwareServiceImpl
    IotProductService <|.. IotOtaFirmwareServiceImpl
```

### 关键方法说明

#### createOtaFirmware
创建新的OTA固件记录。执行以下步骤：
1. 验证固件产品ID和版本号的唯一性（防止重复）
2. 验证关联的产品是否存在
3. 将VO转换为DO对象
4. 下载固件文件并计算MD5签名和文件大小
5. 保存固件记录到数据库
6. 返回生成的固件ID

#### updateOtaFirmware
更新现有的OTA固件记录。执行以下步骤：
1. 验证固件是否存在
2. 将更新VO转换为DO对象
3. 更新数据库记录

#### getOtaFirmwareById
根据ID获取固件记录

#### getOtaFirmwareByProductIdAndVersion
根据产品ID和版本号获取固件记录

#### getOtaFirmwareList
根据ID列表批量获取固件记录

#### getOtaFirmwarePage
根据分页条件获取固件记录列表

#### validateFirmwareExists
验证固件是否存在，不存在则抛出异常

#### calculateFileDigest
私有方法，用于计算固件文件的MD5签名：
1. 从指定URL下载文件内容
2. 设置文件大小属性
3. 计算MD5签名并设置到固件对象

## 与其他模块的关系

### 上游依赖
- [产品管理模块](product_management.md)：提供产品验证服务
- [数据访问层](data_access_layer.md)：提供固件数据的CRUD操作

### 下游依赖
- [OTA任务管理模块](ota_task_management.md)：使用固件信息创建升级任务
- [OTA任务记录模块](ota_task_records.md)：记录固件升级执行情况
- [OTA固件控制器](ota_firmware_controller.md)：提供RESTful API接口

### 关联数据表
- `iot_ota_firmware`：存储固件元信息和文件签名
- 关联 `iot_product` 表通过 product_id 外键

## 接口规范

### 创建固件
- **方法**：`createOtaFirmware`
- **参数**：`IotOtaFirmwareCreateReqVO`
- **返回**：固件ID (Long)
- **异常**：
  - `OTA_FIRMWARE_PRODUCT_VERSION_DUPLICATE`：产品ID和版本号组合重复
  - 产品不存在异常：来自产品服务的验证异常

### 更新固件
- **方法**：`updateOtaFirmware`
- **参数**：`IotOtaFirmwareUpdateReqVO`
- **返回**：无
- **异常**：`OTA_FIRMWARE_NOT_EXISTS`：固件不存在

### 查询固件
- **方法**：`getOtaFirmware`
- **参数**：固件ID (Long)
- **返回**：固件详情 (IotOtaFirmwareDO)
- **异常**：`OTA_FIRMWARE_NOT_EXISTS`：固件不存在

- **方法**：`getOtaFirmwareByProductIdAndVersion`
- **参数**：产品ID (Long), 版本号 (String)
- **返回**：固件详情 (IotOtaFirmwareDO)

- **方法**：`getOtaFirmwareList`
- **参数**：固件ID集合 (Collection<Long>)
- **返回**：固件列表 (List<IotOtaFirmwareDO>)

- **方法**：`getOtaFirmwarePage`
- **参数**：分页请求 (IotOtaFirmwarePageReqVO)
- **返回**：分页结果 (PageResult<IotOtaFirmwareDO>)

## 异常处理

模块定义了以下业务异常：
- `OTA_FIRMWARE_NOT_EXISTS`：固件不存在
- `OTA_FIRMWARE_PRODUCT_VERSION_DUPLICATE`：固件产品和版本重复

这些异常通过 `ServiceExceptionUtil.exception()` 方法抛出，统一由全局异常处理器处理。

## 安全考虑

1. **输入验证**：所有输入参数通过VO对象进行基本验证
2. **业务规则验证**：固件产品和版本号的唯一性验证防止数据重复
3. **文件安全**：仅从配置的可信URL下载固件文件，计算签名确保文件完整性
4. **权限控制**：通过控制器层的权限注解进行访问控制（在API层实现）

## 性能考虑

1. **数据库操作**：使用MyBatis Plus进行高效的数据库操作
2. **文件下载**：文件下载和签名计算在创建固件时进行，避免重复计算
3. **缓存潜力**：固件信息变化不频繁，可考虑添加缓存层提高查询性能
4. **分页查询**：所有列表查询支持分页，防止一次性加载过大数据量

## 配置说明

该模块主要依赖以下配置：
- 无特殊配置项，使用Spring默认配置
- 依赖数据源配置通过MyBatis Plus自动装配
- 依赖的产品服务通过Spring容器注入

## 与其他模块的集成点

### 与OTA任务管理的集成
OTA任务创建时会调用此模块获取固件信息：
```java
// 在OTA任务服务中
IotOtaFirmwareDO firmware = otaFirmwareService.getOtaFirmware(firmwareId);
// 使用固件信息创建升级任务
```

### 与设备管理的集成
设备上报当前固件版本时，平台会查询此模块获取最新固件信息进行版本比较：
```java
// 在设备服务中
IotOtaFirmwareDO latestFirmware = otaFirmwareService.getOtaFirmwareByProductIdAndVersion(
    device.getProductId(), 
    "latest" // 或特定版本查询逻辑
);
// 比较设备当前版本与最新固件版本
```

## 开发指南

### 添加新功能
1. 在VO层添加新的请求/响应字段
2. 在DO层添加对应的数据库字段（通过MyBatis Plus自动映射）
3. 在Mapper层添加所需的查询方法
4. 在Service层实现业务逻辑
5. 在Controller层暴露API接口

### 常见问题
1. **文件下载失败**：确保固件URL可访问，网络正常
2. **签名计算异常**：检查文件是否被篡改或下载不完整
3. **版本重复错误**：在创建前先前检查版本唯一性后重试操作
4. **产品不存在**：确保产品ID正确且产品已在产品管理模块中创建

## 测试考虑

1. **单元测试**：
   - 测试固件创建的正常流程和异常流程（版本重复、产品不存在）
   - 测试固件更新功能
   - 测试各种查询方法
   - 测试文件签名计算功能（可使用mock HTTP客户端）

2. **集成测试**：
   - 测试与产品服务的集成
   - 测试与数据访问层的集成
   - 测试完整的固件生命周期（创建-查询-更新-删除）

3. **性能测试**：
   - 测试大文件下载和签名计算的性能
   - 测试并发固件创建的处理能力
   - 测试分页查询的性能表现

## 变更历史

| 版本 | 日期 | 作者 | 描述 |
|------|------|------|------|
| V1.0 | 2023-10-15 | Shelly Chan | 初始版本，实现基本的固件CRUD操作和文件签名计算 |

## 参考文档

- [产品管理模块](product_management.md)：了解产品信息管理
- [OTA任务管理模块](ota_task_management.md)：了解如何使用固件创建升级任务
- [数据访问层指南](data_access_layer.md)：了解MyBatis Plus使用规范
- [异常处理机制](exception_handling.md)：了解统一异常处理设计