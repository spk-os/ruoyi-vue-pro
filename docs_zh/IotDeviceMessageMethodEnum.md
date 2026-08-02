# IotDeviceMessageMethodEnum 枚举文档

## 概述

`IotDeviceMessageMethodEnum` 是 IoT 核心模块中定义的消息方法枚举，涵盖了设备与平台之间所有可能的消息交互类型。

## 枚举值分类

### 设备状态
- `STATE_UPDATE`：设备状态更新（上行）

### 拓扑管理
- `TOPO_ADD`：添加拓扑关系（下行）
- `TOPO_DELETE`：删除拓扑关系（下行）
- `TOPO_GET`：获取拓扑关系（下行）
- `TOPO_CHANGE`：拓扑关系变更通知（上行）

### 设备注册
- `DEVICE_REGISTER`：设备动态注册（上行）
- `SUB_DEVICE_REGISTER`：子设备动态注册（上行）

### 设备属性
- `PROPERTY_POST`：属性上报（上行）
- `PROPERTY_SET`：属性设置（下行）
- `PROPERTY_PACK_POST`：批量上报（属性 + 事件 + 子设备）（上行，网关独有）

### 设备事件
- `EVENT_POST`：事件上报（上行）

### 设备服务调用
- `SERVICE_INVOKE`：服务调用（下行）

### 设备配置
- `CONFIG_PUSH`：配置推送（下行）← config_53 模块相关

### OTA 固件
- `OTA_UPGRADE`：OTA 固件信息推送（下行）
- `OTA_PROGRESS`：OTA 升级进度上报（上行）

## 特殊方法

### 回复禁用方法
以下方法不进行回复：
- `STATE_UPDATE`
- `OTA_PROGRESS`
