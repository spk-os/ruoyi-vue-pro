# 消息总线系统文档

## 概述

消息总线系统是 IoT 核心模块的基础设施，负责设备消息的分发和路由。config_53 模块通过消息总线实现配置推送的下发。

## 支持的消息总线类型

| 类型 | 描述 | 适用场景 |
|------|------|----------|
| local | 本地内存消息总线 | 单机部署、测试环境 |
| redis | Redis 消息总线 | 需要高性能、低延迟的场景 |
| rocketmq | RocketMQ 消息总线 | 需要高可靠、消息队列的场景 |
| kafka | Kafka 消息总线 | 需要高吞吐、大数据量的场景 |
| rabbitmq | RabbitMQ 消息总线 | 需要灵活路由的场景 |

## 配置示例

```yaml
yudao:
  iot:
    message-bus:
      type: local  # 可选: local, redis, rocketmq, kafka, rabbitmq
```

## 与 config_53 的集成

config_53 模块通过消息总线发送配置推送消息：

1. 创建 `IotDeviceConfigPushReqDTO` 对象
2. 封装到 `IotDeviceMessage` 中，设置方法为 `CONFIG_PUSH`
3. 通过消息总线发送到设备的 topic
4. 设备订阅对应的 topic 接收消息
