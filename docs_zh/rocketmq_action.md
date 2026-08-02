# RocketMQ Action Module Documentation

## Overview

The RocketMQ Action module provides the implementation for sending IoT device messages to Apache RocketMQ, a distributed messaging and streaming platform. This enables reliable, high-throughput message delivery for IoT applications requiring scalable messaging infrastructure.

## Core Components

### IotRocketMQDataRuleAction

The main class implementing the `IotDataRuleAction` interface for RocketMQ operations.

```java
@ConditionalOnClass(name = "org.apache.rocketmq.client.producer.DefaultMQProducer")
@Component
@Slf4j
public class IotRocketMQDataRuleAction extends IotDataRuleCacheableAction<IotDataSinkRocketMQConfig, DefaultMQProducer>
```

## Configuration

The RocketMQ Action requires configuration stored in `IotDataSinkRocketMQConfig`:

```java
public class IotDataSinkRocketMQConfig {
    private String group;         // Producer group name
    private String nameServer;    // RocketMQ Name Server address
    private String topic;         // Topic to publish messages to
    private String tags;          // Message tags for filtering
}
```

## Usage Example

```java
IotDataSinkRocketMQConfig config = new IotDataSinkRocketMQConfig();
config.setGroup("iot_producer_group");
config.setNameServer("localhost:9876");
config.setTopic("iot_device_messages");
config.setTags("device_data");
```

## Implementation Details

### Producer Management

The action manages RocketMQ producers with proper lifecycle management:

```java
@Override
protected DefaultMQProducer initProducer(IotDataSinkRocketMQConfig config) throws Exception {
    DefaultMQProducer producer = new DefaultMQProducer(config.getGroup());
    producer.setNamesrvAddr(config.getNameServer());
    producer.start();
    return producer;
}

@Override
protected void closeProducer(DefaultMQProducer producer) {
    producer.shutdown();
}
```

### Message Processing

The execute method handles message formatting and sending:

```java
@Override
public void execute(IotDeviceMessage message, IotDataSinkRocketMQConfig config) throws Exception {
    // 1. Get or create Producer from cache
    DefaultMQProducer producer = getProducer(config);

    // 2.1 Create message object with Topic, Tag, and message body
    Message msg = new Message(config.getTopic(), config.getTags(), JsonUtils.toJsonByte(message));
    
    // 2.2 Send synchronous message and process result
    SendResult sendResult = producer.send(msg);
    
    // 2.3 Handle sending result
    if (SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
        log.info("[execute][message({}) config({}) 发送成功，结果({})]", message, config, sendResult);
    } else {
        log.error("[execute][message({}) config({}) 发送失败，结果({})]", message, config, sendResult);
    }
}
```

## Error Handling

}
```

## Message Format

Messages are serialized to JSON before sending to RocketMQ:

```java
// Message body is the JSON representation of IotDeviceMessage
Message msg = new Message(config.getTopic(), config.getTags(), JsonUtils.toJsonByte(message));
```

## Features

1. **Reliable Delivery**: Uses synchronous sending with result verification
2. **Topic-Based Messaging**: Messages published to specific topics
3. **Tag Filtering**: Supports message tags for consumer-side filtering
4. **Producer Grouping**: Organizes producers into logical groups
5. **Automatic Reconnection**: Leverages RocketMQ client's built-in reconnection capabilities
6. **Connection Caching**: Uses Guava Cache for producer instance reuse

## Error Handling

The RocketMQ Action includes comprehensive error handling:

```java
try {
    // Producer acquisition and message sending
    if (SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
        log.info("[execute][message({}) config({}) 发送成功，结果({})]", message, config, sendResult);
    } else {
        log.error("[execute][message({}) config({}) 发送失败，结果({})]", message, config, sendResult);
    }
} catch (Exception e) {
    log.error("[execute][message({}) config({}) 发送异常]", message, config, e);
    throw e;
}
```

## Performance Considerations

- Producer instance caching via Guava Cache (30-minute expiration)
- Synchronous sending with configurable timeout
- Efficient JSON serialization
- Minimal object creation during message processing
- Leverages RocketMQ's high-performance client library

## Integration

The RocketMQ Action integrates with the IoT rule engine through the `IotDataRuleAction` interface. When a rule condition is met with a RocketMQ sink configured, the rule engine invokes this action to send device messages to the specified RocketMQ topic.

## Configuration Properties

| Property | Description | Required |
|----------|-------------|----------|
| group | Producer group identifier | Yes |
| nameServer | RocketMQ Name Server address (host:port) | Yes |
| topic | Target topic for message publishing | Yes |
| tags | Optional message tags for filtering | No |

## See Also

- [Action Module Overview](action.md)
- [Apache RocketMQ Documentation](https://rocketmq.apache.org/docs/)
- [RocketMQ Java Client Guide](https://rocketmq.apache.org/docs/quick-start/)
- [IoT Data Rule Engine](https://github.com/YunaiV/yudao-cloud/tree/master/yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/data/IotDataRuleServiceImpl.java)