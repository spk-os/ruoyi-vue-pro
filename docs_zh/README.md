# IoT 核心模块文档

## 文档列表

| 文件名 | 描述 |
|--------|------|
| [config_53.md](config_53.md) | IoT 设备配置推送模块主文档 |
| [IotDeviceMessageMethodEnum.md](IotDeviceMessageMethodEnum.md) | 消息方法枚举文档 |
| [ota.md](ota.md) | OTA 升级模块文档 |
| [message-bus.md](message-bus.md) | 消息总线系统文档 |

## 模块关系

```
config_53 (设备配置推送)
├── 依赖 → IotDeviceMessageMethodEnum (消息方法枚举)
├── 依赖 → message-bus (消息总线系统)
└── 关联 → OTA (OTA 升级模块)
```
