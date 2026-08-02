# IoT 网关上游模块文档

## 1. 概述

IoT 网关上游模块（Upstream Module）是 IoT 网关的核心组件之一，负责处理来自设备的各种上游请求。该模块实现了多种协议（CoAP、HTTP、TCP、MQTT、UDP、WebSocket 等）的协议解析、消息路由和业务处理，是设备与云端系统之间的桥梁。

上游模块的主要功能包括：
- 设备认证与授权
- 子设备动态注册
- 设备消息上报
- 设备属性上报
- 设备服务调用
- 设备拓扑管理

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        IoT 网关                                 │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│  │   CoAP 协议 │  │   HTTP 协议 │  │   TCP 协议 │  │ MQTT 协议│ │
│  │  处理层     │  │  处理层     │  │  处理层     │  │  处理层 │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬───┘ │
│         │                │                │                │    │
│         ▼                ▼                ▼                ▼    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              上游请求处理器（Upstream Handler）          │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │   │
│  │  │ 子设备注册  │  │  消息上报   │  │  属性上报   │      │   │
│  │  │ 处理器      │  │  处理器     │  │  处理器     │      │   │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘      │   │
│  │         │                │                │             │   │
│  │         ▼                ▼                ▼             │   │
│  │  ┌─────────────────────────────────────────────────┐   │   │
│  │  │              设备服务调用（Device Service）     │   │   │
│  │  └─────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块组成

上游模块主要由以下部分组成：

1. **协议处理层**：负责不同协议的请求解析和转发
   - CoAP 协议处理
   - HTTP 协议处理
   - TCP 协议处理
   - MQTT 协议处理
   - UDP 协议处理
   - WebSocket 协议处理

2. **上游请求处理器**：处理具体的业务请求
   - 子设备注册处理器
   - 消息上报处理器
   - 属性上报处理器
   - 设备服务调用处理器
   - 设备拓扑管理处理器

3. **设备服务层**：与后端 IoT 核心服务交互
   - 设备认证服务
   - 设备注册服务
   - 消息存储服务
   - 属性存储服务
   - 服务调用服务

## 3. 核心组件

### 3.1 IotCoapRegisterSubHandler

**文件路径**：`yudao-module-iot/yudao-module-iot-gateway/src/main/java/cn/iocoder/yudao/module/iot/gateway/protocol/coap/handler/upstream/IotCoapRegisterSubHandler.java`

**功能描述**：
CoAP 协议的子设备动态注册处理器。用于子设备通过 CoAP 协议向网关进行动态注册，需要网关认证。

**类图**：

```
┌─────────────────────────────────────────────────────────────────┐
│                    IotCoapRegisterSubHandler                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ - deviceApi: IotDeviceCommonApi                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│  + handle0(CoapExchange): CommonResult                        │
│  + requiresAuthentication(): boolean                          │
│  + getProductKey(List<String>): String                        │
│  + getDeviceName(List<String>): String                        │
│  + deserializeRequest(...): T                                 │
│                                                         │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  SubDeviceRegisterRequest (静态内部类)                  │   │
│  │  - params: List<IotSubDeviceRegisterReqDTO>             │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

**处理流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│                    子设备注册处理流程                           │
├─────────────────────────────────────────────────────────────────┤
│  1. 解析 URI 路径获取网关设备信息（productKey, deviceName）     │
│     └─ 路径格式：/auth/register/sub-device/{productKey}/{deviceName} │
│                                                              │
│  2. 解析请求体获取子设备列表                                  │
│     └─ SubDeviceRegisterRequest.params: List<IotSubDeviceRegisterReqDTO> │
│                                                              │
│  3. 构建注册请求 DTO                                          │
│     └─ IotSubDeviceRegisterFullReqDTO                        │
│         - gatewayProductKey: productKey                       │
│         - gatewayDeviceName: deviceName                       │
│         - subDevices: request.params                          │
│                                                              │
│  4. 调用设备服务进行子设备注册                                │
│     └─ deviceApi.registerSubDevices(reqDTO)                   │
│         └─ CommonResult<List<IotSubDeviceRegisterRespDTO>>   │
│                                                              │
│  5. 检查错误并返回结果                                        │
│     └─ result.checkError()                                   │
│     └─ return success(result.getData())                       │
└─────────────────────────────────────────────────────────────────┘
```

**关键代码说明**：

```java
// 从 URI 路径解析网关设备信息
String productKey = getProductKey(uriPath);  // 路径索引 3
String deviceName = getDeviceName(uriPath);  // 路径索引 4

// 解析子设备列表
SubDeviceRegisterRequest request = deserializeRequest(exchange, SubDeviceRegisterRequest.class);

// 构建完整注册请求
IotSubDeviceRegisterFullReqDTO reqDTO = new IotSubDeviceRegisterFullReqDTO()
    .setGatewayProductKey(productKey)
    .setGatewayDeviceName(deviceName)
    .setSubDevices(request.getParams());

// 调用设备服务注册子设备
CommonResult<List<IotSubDeviceRegisterRespDTO>> result = deviceApi.registerSubDevices(reqDTO);
```

### 3.2 SubDeviceRegisterRequest

**功能描述**：CoAP 子设备注册请求的 DTO 对象，包含子设备列表。

```java
@Data
public static class SubDeviceRegisterRequest {
    private List<IotSubDeviceRegisterReqDTO> params;
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| params | List<IotSubSubDeviceRegisterReqDTO> | 子设备注册信息列表 |

### 3.3 其他上游处理器

除了 CoAP 子设备注册处理器外，上游模块还包括其他协议的处理器：

| 处理器名称 | 协议 | 功能 |
|-----------|------|------|
| IotHttpRegisterSubHandler | HTTP | HTTP 协议的子设备动态注册 |
| IotTcpRegisterSubHandler | TCP | TCP 协议的子设备动态注册 |
| IotMqttRegisterSubHandler | MQTT | MQTT 协议的子设备动态注册 |

## 4. 数据流

### 4.1 子设备注册数据流

```
┌─────────────┐      ┌──────────────────┐      ┌──────────────────────┐      ┌─────────────┐
│   子设备    │──────▶│   IoT 网关       │──────▶│  设备服务 (IotDeviceCommonApi) │──────▶│  IoT 核心服务 │
│ (CoAP 协议) │      │ (IotCoapRegisterSubHandler) │      │ (registerSubDevices) │      │ (注册子设备) │
└─────────────┘      └──────────────────┘      └──────────────────────┘      └─────────────┘
       ▲                    ▲                    ▲                    ▲                    │
       │                    │                    │                    │                    │
       │ 响应结果           │ 解析请求           │ 构建 DTO           │ 调用服务           │ 注册完成
       │ ◀────────────────│◀───────────────────│◀───────────────────│◀───────────────────┘
       │
┌──────┴──────┐
│  子设备注册响应 │
│ (IotSubDeviceRegisterRespDTO) │
└─────────────┘
```

### 4.2 请求处理流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    请求处理流程                                 │
├─────────────────────────────────────────────────────────────────┤
│  1. 接收 CoAP 请求                                              │
│     └─ CoapExchange exchange                                    │
│                                                              │
│  2. 验证认证（requiresAuthentication = true）                   │
│                                                              │
│  3. 解析 URI 路径                                               │
│     └─ exchange.getRequestOptions().getUriPath()               │
│     └─ 提取 productKey (索引 3) 和 deviceName (索引 4)         │
│                                                              │
│  4. 反序列化请求体                                              │
│     └─ deserializeRequest(exchange, SubDeviceRegisterRequest.class) │
│                                                              │
│  5. 参数校验                                                    │
│     └─ Assert.notNull(request, "请求参数不能为空")             │
│     └─ Assert.notEmpty(request.getParams(), "params 不能为空") │
│                                                              │
│  6. 构建注册请求 DTO                                            │
│     └─ IotSubDeviceRegisterFullReqDTO                        │
│                                                              │
│  7. 调用设备服务进行注册                                        │
│     └─ deviceApi.registerSubDevices(reqDTO)                   │
│                                                              │
│  8. 错误检查                                                    │
│     └─ result.checkError()                                   │
│                                                              │
│  9. 返回响应                                                    │
│     └─ return success(result.getData())                       │
└─────────────────────────────────────────────────────────────────┘
```

## 5. 依赖关系

### 5.1 模块依赖

上游模块依赖于以下模块：

| 模块 | 依赖说明 |
|------|----------|
| yudao-module-iot-core | 提供设备 API、DTO 定义、主题模型等核心功能 |
| yudao-module-iot-gateway | 网关模块本身，包含协议处理框架 |
| yudao-framework-common | 提供通用工具类、结果对象等 |
| Spring Framework | 提供依赖注入、Web 支持等 |

### 5.2 类依赖关系

```
┌─────────────────────────────────────────────────────────────────┐
│                    类依赖关系                                   │
├─────────────────────────────────────────────────────────────────┤
│ IotCoapRegisterSubHandler                                      │
│   ├─ extends IotCoapAbstractHandler                            │
│   ├─ depends on: IotDeviceCommonApi                            │
│   ├─ depends on: SpringUtil                                    │
│   ├─ depends on: CollUtil                                      │
│   ├─ depends on: Assert                                        │
│   └─ uses: SubDeviceRegisterRequest (内部类)                   │
│                                                              │
│ SubDeviceRegisterRequest                                       │
│   └─ uses: IotSubDeviceRegisterReqDTO                          │
│                                                              │
│ IotDeviceCommonApi (远程调用)                                  │
│   └─ registerSubDevices(IotSubDeviceRegisterFullReqDTO)        │
│     └─ returns: CommonResult<List<IotSubDeviceRegisterRespDTO>>│
└─────────────────────────────────────────────────────────────────┘
```

## 6. 配置说明

### 6.1 CoAP 协议配置

CoAP 协议的路径配置：

```
/auth/register/sub-device/{productKey}/{deviceName}
```

- `productKey`：网关设备的 ProductKey（URI 路径索引 3）
- `deviceName`：网关设备的 DeviceName（URI 路径索引 4）

### 6.2 认证配置

该处理器需要认证（`requiresAuthentication()` 返回 `true`），网关设备需要先通过认证才能进行子设备注册。

## 7. 错误处理

### 7.1 常见错误

| 错误代码 | 错误信息 | 处理建议 |
|----------|----------|----------|
| 空参数 | "请求参数不能为空" | 检查请求体是否包含有效的 SubDeviceRegisterRequest |
| params 为空 | "params 不能为空" | 确保请求体中包含子设备列表 |
| 注册失败 | 设备服务返回错误 | 检查设备服务日志，确认子设备注册是否成功 |

### 7.2 错误处理流程

```java
CommonResult<List<IotSubDeviceRegisterRespDTO>> result = deviceApi.registerSubDevices(reqDTO);
result.checkError();  // 如果 result 包含错误，抛出异常
return success(result.getData());  // 返回注册成功的子设备列表
```

## 8. 扩展性设计

### 8.1 协议扩展

通过继承 `IotCoapAbstractHandler`，可以轻松扩展其他协议的上游处理器：

```java
// HTTP 协议示例
public class IotHttpRegisterSubHandler extends IotHttpAbstractHandler {
    // 实现类似的子设备注册逻辑
}

// TCP 协议示例
public class IotTcpRegisterSubHandler extends IotTcpAbstractHandler {
    // 实现类似的子设备注册逻辑
}
```

### 8.2 功能扩展

通过实现不同的业务处理器，可以扩展其他上游功能：

- 消息上报处理器
- 属性上报处理器
- 设备服务调用处理器
- 设备拓扑管理处理器

## 9. 参考文档

- [阿里云 - 动态注册子设备](https://help.aliyun.com/zh/iot/user-guide/register-devices)
- [IoT 网关模块文档](../iot-gateway.md)
- [IoT 核心模块文档](../iot-core.md)
- [设备服务 API 文档](../iot-device-api.md)

## 10. 版本信息

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| 1.0.0 | 2024-01-01 | 芋道源码 | 初始版本 |
