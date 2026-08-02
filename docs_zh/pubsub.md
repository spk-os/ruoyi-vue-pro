# Pub/Sub 模块文档

## 概述

Pub/Sub 模块是 Yudao 框架中基于 Redis 实现的消息队列解决方案的一部分，专门处理 Redis 的发布/订阅（Pub/Sub）模式。该模块提供了抽象基类和自动配置，使得开发者可以轻松实现基于 Redis Pub/Sub 的异步通信机制。

在 Yudao 框架中，MQ（消息队列）功能被划分为两种主要实现方式：
1. **Pub/Sub（发布/订阅）**：基于 Redis 的发布/订阅功能，适用于广播场景（一对应用场景
2. **Stream（流）**：基于 Redis Stream 功能，提供更可靠的消息队列特性，如消息持久化、消费组、重试机制等

本文档重点介绍 Pub/Sub 相关的实现。

## 核心组件

### AbstractRedisChannelMessageListener

Redis Pub/Sub 监听器抽象类，用于实现广播消费。

**位置**：`yudao-framework/yudao-spring-boot-starter-mq/src/main/java/cn/iocoder/yudao/framework/mq/redis/core/pubsub/AbstractRedisChannelMessageListener.java`

**关键特性**：
- 实现 Spring Data Redis 的 `MessageListener` 接口
- 自动解析消息类型（通过泛型）
- 自动获取 Redis Channel 名称（默认使用消息类的简单名称）
- 支持消息拦截器（前置和后置处理）
- 需要子类实现具体的消息处理逻辑

**核心方法**：
- `onMessage(Message message, byte[] bytes)`：Spring Data Redis 回调方法，负责消息的反序列化和分发
- `onMessage(T message)`：抽象方法，子类必须实现以处理具体的消息业务逻辑
- `consumeMessageBefore/After`：调用拦截器进行消息前置和后置处理
- `getMessageClass()`：通过反射获取泛型 T 的实际类型

**使用示例**：
```java
@Component
public class TestMessageListener extends AbstractRedisChannelMessageListener<TestMessage> {
    
    @Override
    public void onMessage(TestMessage message) {
        // 处理消息业务逻辑
        System.out.println("收到消息: " + message.getContent());
    }
}
```

### AbstractRedisChannelMessage

Redis Pub/Sub 消息的抽象基类。

**位置**：`yudao-framework/yudao-spring-boot-starter-mq/src/main/java/cn/iocoder/yudao/framework/mq/redis/core/pubsub/AbstractRedisChannelMessage.java`

**关键特性**：
- 继承自 `AbstractRedisMessage`，拥有消息头功能
- 提供 `getChannel()` 方法，默认返回消息类的简单名称作为 Channel 名称
- 使用 `@JsonIgnore` 注解避免在序列化时包含 channel 信息（因为发布时已经指定了 channel）

**使用示例**：
```java
@Data
@EqualsAndHashCode(callSuper = true)
public class TestMessage extends AbstractRedisChannelMessage {
    private String content;
    
    // Channel 名称将默认为 "TestMessage"，可通过重写 getChannel() 方法自定义
}
```

### RedisMQTemplate

Redis 消息模板，用于发送 Pub/Sub 消息。

**位置**：`yudao-framework/yudao-spring-boot-starter-mq/src/main/java/cn/iocoder/yudao/framework/mq/redis/core/RedisMQTemplate.java`

**关键特性**：
- 封装了 RedisTemplate 的操作
- 提供 `send(T message)` 方法用于发送 Pub/Sub 消息
- 支持消息拦截器（发送前后处理）
- 自动将消息对象序列化为 JSON 字符串进行发送

**使用示例**：
```java
@Service
public class TestMessageProducer {
    
    @Autowired
    private RedisMQTemplate redisMQTemplate;
    
    public void sendMessage(TestMessage message) {
        redisMQTemplate.send(message);
    }
}
```

### RedisMessageInterceptor

消息拦截器接口，用于在消息发送和消费过程中进行前置和后置处理。

**位置**：`yudao-framework/yudao-spring-boot-starter-mq/src/main/java/cn/iocoder/yudao/framework/mq/redis/core/interceptor/RedisMessageInterceptor.java`

**关键特性**：
- 提供四个默认方法：`sendMessageBefore/After` 和 `consumeMessageBefore/After`
- 允许开发者自定义拦截逻辑，如日志、事务、安全检查等
- 在 RedisMQTemplate 中被调用，实现 AOP 风格的拦截

**使用示例**：
```java
@Component
public class LoggingInterceptor implements RedisMessageInterceptor {
    
    @Override
    public void sendMessageBefore(AbstractRedisMessage message) {
        System.out.println("发送消息前: " + message);
    }
    
    @Override
    public void consumeMessageAfter(AbstractRedisMessage message) {
        System.out.println("消费消息后: " + message);
    }
}
```

## 自动配置

### YudaoRedisMQConsumerAutoConfiguration

Redis Pub/Sub 消费者的自动配置类。

**位置**：`yudao-framework/yudao-spring-boot-starter-mq/src/main/java/cn/iocoder/yudao/framework/mq/redis/config/YudaoRedisMQConsumerAutoConfiguration.java`

**关键特性**：
- 依赖于 `YudaoRedisAutoConfiguration`（Redis 基础配置）
- 当存在 `AbstractRedisChannelMessageListener` Bean 时自动激活
- 创建 `RedisMessageListenerContainer` 并注册所有监听器
- 使用 `ChannelTopic` 将监听器与对应的 Redis Channel 绑定
- 支持消息拦截器的注入和设置

### YudaoRedisMQProducerAutoConfiguration

Redis Pub/Sub 生产者的自动配置类。

**位置**：`yudao-framework/yudao-spring-boot-starter-mq/src/main/java/cn/iocoder/yudao/framework/mq/redis/config/YudaoRedisMQProducerAutoConfiguration.java`

**关键特性**：
- 依赖于 `YudaoRedisAutoConfiguration`（Redis 基础配置）
- 创建 `RedisMQTemplate` Bean 并注入消息拦截器
- 为消息发送提供统一的模板

## 工作流程

### 消息发送流程

1. 开发者调用 `RedisMQTemplate.send(message)` 方法
2. 方法内部先调用所有拦截器的 `sendMessageBefore` 方法（正序）
3. 使用 RedisTemplate 将消息对象序列化为 JSON 并发送到指定的 Redis Channel
4. 方法内部后调用所有拦截器的 `sendMessageAfter` 方法（倒序）

### 消息消费流程

1. Redis 消息到达对应的 Channel
2. Spring Data Redis 的 `RedisMessageListenerContainer` 接收到消息
3. 调用 `AbstractRedisChannelMessageListener.onMessage(Message message, byte[] bytes)` 方法
4. 方法内部将消息体反序列化为指定类型的消息对象
5. 调用所有拦截器的 `consumeMessageBefore` 方法（正序）
6. 调用抽象方法 `onMessage(T message)`，由子类实现具体业务逻辑
7. 调用所有拦截器的 `consumeMessageAfter` 方法（倒序）

## 与 Stream 模块的区别

虽然 Pub/Sub 和 Stream 都是基于 Redis 的消息队列实现，但它们有显著的区别：

| 特性 | Pub/Sub | Stream |
|------|---------|--------|
| 消息持久性 | 不持久化，订阅者离线期间发送的消息会丢失 | 持久化，消息会保存在 Redis 中直到被显式删除 |
| 消费模式 | 广播模式，所有订阅者都会收到消息 | 消费组模式，消息由组内的一个消费者处理 |
| 重试机制 | 无内置重试机制 | 支持消息确认和未确认消息的重新传递 |
| 适用场景 | 实时广播、通知、日志等对可靠性要求不高的场景 | 任务队列、事件溯源、需要可靠传递的业务场景 |
| 实现类 | AbstractRedisChannelMessageListener / AbstractRedisChannelMessage | AbstractRedisStreamMessageListener / AbstractRedisStreamMessage |

## 最佳实践

1. **消息类设计**：
   - 继承 `AbstractRedisChannelMessage` 并添加必要的业务字段
   - 如需自定义 Channel 名称，重写 `getChannel()` 方法
   - 使用 Lombok 的 `@Data` 注解简化代码

2. **监听器实现**：
   - 继承 `AbstractRedisChannelMessageListener<YourMessageType>`
   - 实现 `onMessage(T message)` 方法处理业务逻辑
   - 保持业务逻辑简洁，复杂逻辑建议委托给服务层

3. **拦截器使用**：
   - 实现 `RedisMessageInterceptor` 接口添加通用处理逻辑
   - 常见用途：日志记录、性能监控、事务管理、异常处理等
   - 注意拦截器的执行顺序（发送时正序，消费时倒序）

4. **配置与依赖**：
   - 确保已引入 `yudao-spring-boot-starter-mq` 依赖
   - Redis 连接配置通过 Spring Boot 的标准方式进行
   - 无需额外配置即可自动装配 Pub/Sub 功能

5. **异常处理**：
   - 在 `onMessage` 方法中捕获并处理业务异常
   - 避免异常传播导致消息丢失（Pub/Sub 模式下没有重试机制）
   - 考虑使用死信队列或补偿机制处理失败消息

## 依赖关系

Pub/Sub 模块依赖于以下核心组件：
- Redis 基础配置 (`YudaoRedisAutoConfiguration`)
- JSON 工具类 (`JsonUtils`)
- Lombok 注解（用于简化代码）
- Spring Data Redis

与其他模块的关系：
- 与 Stream 模块共享基础设施（如 `RedisMQTemplate`, `RedisMessageInterceptor`, `AbstractRedisMessage`）
- 都属于 `yudao-spring-boot-starter-mq` 启动器的一部分
- 可以根据业务需求选择使用 Pub/Sub 或 Stream 实现

## 使用场景推荐

### 适合使用 Pub/Sub 的场景：
- 实时通知系统（如消息提醒、系统广播）
- 日志收集和监控
- 缓存失效通知
- 微服务间的事件广播（对可靠性要求不高的场景）
- 简单的解耦通知机制

### 不适合使用 Pub/Sub 的场景（应考虑 Stream 或其他可靠MQ）：
- 金融交易等对消息可靠性要求极高的场景
- 任务队列需要确保每条消息都被处理的场景
- 需要消息持久化和重试机制的场景
- 消息顺序性要求严格的场景

## 示例代码

完整的 Pub/Sub 使用示例：

```java
// 1. 定义消息类
@Data
@EqualsAndHashCode(callSuper = true)
public class UserLoginMessage extends AbstractRedisChannelMessage {
    private Long userId;
    private String loginIp;
    private Date loginTime;
}

// 2. 实现消息监听器
@Component
public class UserLoginListener extends AbstractRedisChannelMessageListener<UserLoginMessage> {
    
    @Autowired
    private UserService userService;
    
    @Override
    public void onMessage(UserLoginMessage message) {
        // 处理用户登录事件，例如更新最后登录时间
        userService.updateLastLoginTime(message.getUserId(), message.getLoginIp(), message.getLoginTime());
    }
}

// 3. 使用拦截器添加日志
@Component
public class MessageLoggingInterceptor implements RedisMessageInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageLoggingInterceptor.class);
    
    @Override
    public void sendMessageBefore(AbstractRedisMessage message) {
        logger.info("准备发送 Pub/Sub 消息: {} 到 Channel: {}", 
                   message.getClass().getSimpleName(), 
                   ((AbstractRedisChannelMessage) message).getChannel());
    }
    
    @Override
    public void consumeMessageAfter(AbstractRedisMessage message) {
        logger.info("成功消费 Pub/Sub 消息: {} 从 Channel: {}", 
                   message.getClass().getSimpleName(), 
                   ((AbstractRedisChannelMessage) message).getChannel());
    }
}

// 4. 发送消息
@Service
public class UserLoginEventPublisher {
    
    @Autowired
    private RedisMQTemplate redisMQTemplate;
    
    public void publishUserLoginEvent(Long userId, String loginIp) {
        UserLoginMessage message = new UserLoginMessage();
        message.setUserId(userId);
        message.setLoginIp(loginIp);
        message.setLoginTime(new Date());
        
        redisMQTemplate.send(message);
    }
}
```

## 性能考量

1. **Channel 订阅数量**：
   - 每个唯一的 Channel 都会创建一个订阅连接
   - 大量不同的 Channel 可能导致 Redis 连接数增加
   - 建议将相关的事件合并到较少的 Channel 中，通过消息类型进行区分

2. **消息大小**：
   - Redis Pub/Sub 对单条消息大小有限制（默认 512MB，但实际建议远小于此值）
   - 大消息应考虑使用其他存储机制（如数据库）并仅发送引用

3. **消费者水平扩展**：
   - Pub/Sub 是广播模式，所有订阅者都会收到所有消息
   - 水平扩展消费者会导致重复消费（每个消费者都会收到完整的消息流）
   - 如需负载均衡消费，应考虑使用 Stream 模式或其他队列实现

4. **网络和延迟**：
   - Pub/Sub 通常具有很低的延迟，适合实时场景
   - 网络抖动可能导致消息丢失，无法保证送达

## 与其他模块的集成

Pub/Sub 模块可以与 Yudao 框架的其他模块无缝集成：

1. **与 Spring 框架集成**：
   - 完全基于 Spring Bean 模型工作
   - 支持 @Autowired 注入依赖
   - 可以在任何 Spring 管理的组件中使用

2. **与日志框架集成**：
   - 通过拦截器可以轻松添加消息发送和消费的日志
   - 支持统一的日志格式和级别控制

3. **与事务管理集成**：
   - 通过拦截器可以在消息发送前后加入事务控制
   - 注意：消息发送本身是非事务的，但可以在业务方法中统一管理事务

4. **与监控系统集成**：
   - 拦截器提供了理想的切入点来收集消息吞吐量、延迟等监控指标
   - 可以集成到 Micrometer、Prometheus 等监控系统

## 常见问题

**Q: 如何自定义 Redis Channel 名称？**  
A: 在消息类中重写 `getChannel()` 方法返回自定义的 Channel 名称。

**Q: Pub/Sub 模式下如何保证消息不丢失？**  
A: Redis Pub/Sub 本身不提供消息持久化。如果需要高可靠性，建议：
   1. 使用 Stream 模式替代 Pub/Sub
   2. 在业务层实现消息持久化和重试机制
   3. 考虑使用专业的消息队列如 RabbitMQ、Kafka 等

**Q: 如何处理消费过程中的异常？**  
A: 在 `onMessage` 方法中捕获异常并进行适当处理。由于 Pub/Sub 没有重试机制，建议：
   1. 将异常消息转发到死信队列或日志系统
   2. 实现补偿机制处理业务失败
   3. 对于关键业务，考虑使用 Stream 模式

**Q: 如何监控 Pub/Sub 的性能？**  
A: 可以通过实现 `RedisMessageInterceptor` 来收集：
   1. 消息发送和消费的时间戳
   2. 处理耗时统计
   3. 错误率和异常信息
   4. 吞吐量监控（每秒处理的消息数）

## 总结

Pub/Sub 模块为 Yudao 框架提供了轻量级、易于使用的 Redis 发布/订阅消息队列实现。通过抽象基类和自动配置，开发者可以快速实现基于 Redis Pub/Sub 的异步通信机制。虽然在可靠性和功能上不如 Stream 模式或专业消息队列，但对于对实时性要求高、可靠性要求中等的场景（如通知、日志、简单事件广播）是一个很好的选择。

在选择使用 Pub/Sub 还是 Stream 时，应根据业务需求的可靠性、持久性、重试需求和消费模式来决定。对于需要消息持久化、确认机制和消费组功能的场景，应优先考虑使用 Stream 模块。