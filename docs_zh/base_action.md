# Base Action Module Documentation

## Overview

The Base Action module provides the foundational caching mechanism for all IoT data rule actions. It implements a generic caching layer that manages the lifecycle of producer/connections for different action types, ensuring efficient resource utilization and automatic cleanup.

## Core Components

### IotDataRuleCacheableAction

The abstract base class that provides caching functionality for all data rule actions.

```java
@Slf4j
public abstract class IotDataRuleCacheableAction<Config, Producer> implements IotDataRuleAction
```

## Purpose

This abstract class solves a common problem in IoT data routing: efficiently managing connections/producers to external systems (MQTT, TCP, HTTP, Redis, etc.) while ensuring proper resource cleanup.

## Key Features

### 1. Producer Caching Mechanism
- Uses Google Guava's `LoadingCache` for efficient producer instance management
- Caches producers based on their configuration objects
- Automatically expires unused producers after 30 minutes of inactivity
- Provides thread-safe access to cached producers

### 2. Automatic Resource Management
- Automatically closes producers when they are evicted from the cache
- Handles exceptions during producer closure gracefully with logging
- Ensures no resource leaks through proper cleanup mechanisms

### 3. Config-Driven Caching
- Cache keys are the configuration objects themselves
- Different configurations get different producer instances
- Identical configurations reuse the same producer

### 4. Exception Handling
- Comprehensive logging for cache operations (creation, retrieval, removal)
- Proper exception propagation to trigger cache invalidation when needed
- Graceful degradation when individual producers fail

## Architecture

### Component Relationships

```
IotDataRuleAction Interface
        ↑
IotDataRuleCacheableAction Abstract Class (This Component)
        ↓
Specific Action Implementations:
  → IotMqttDataRuleAction
  → IotTcpDataRuleAction  
  → IotHttpDataSinkAction
  → IotRedisRuleAction
  → IotWebSocketDataRuleAction
  → IotRocketMQDataRuleAction
  → IotKafkaDataRuleAction
  → IotDatabaseDataRuleAction
  → IotRabbitMQDataRuleAction
```

### Cache Mechanism Flow

1. **Action Invocation**: When an action is executed, it calls `getProducer(config)`
2. **Cache Lookup**: The cache checks if a producer exists for the given configuration
3. **Cache Hit**: Returns existing producer if available and not expired
4. **Cache Miss**: Creates new producer via `initProducer(config)` method
5. **Producer Usage**: Returns the producer for message sending
6. **Cache Eviction**: After 30 minutes of inactivity, entry is removed
7. **Resource Cleanup**: `removalListener` calls `closeProducer(producer)` to release resources

## Implementation Details

### Cache Configuration

```java
private final LoadingCache<Config, Producer> PRODUCER_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(30)) // 30 minutes idle timeout
        .removalListener((RemovalListener<Config, Producer>) notification -> {
            Producer producer = notification.getValue();
            try {
                closeProducer(producer);
                log.info("[PRODUCER_CACHE][配置({}) 对应的 producer 已关闭]", notification.getKey());
            } catch (Exception e) {
                log.error("[PRODUCER_CACHE][配置({}) 对应的 producer 关闭失败]", notification.getKey(), e);
            }
        })
        .build(new CacheLoader<Config, Producer>() {
            @Override
            public Producer load(Config config) throws Exception {
                try {
                    Producer producer = initProducer(config);
                    log.info("[PRODUCER_CACHE][配置({}) 对应的 producer 已创建并启动]", config);
                    return producer;
                } catch (Exception e) {
                    log.error("[PRODUCER_CACHE][配置({}) 对应的 producer 创建启动失败]", config, e);
                    throw e; // Propagate exception to trigger cache loading failure
                }
            }
        });
```

### Core Methods

#### `getProducer(Config config)`
Retrieves a producer from the cache, creating it if necessary:
```java
protected Producer getProducer(Config config) throws Exception {
    return PRODUCER_CACHE.get(config);
}
```

#### `invalidateProducer(Config config)`
Forces removal of a producer from the cache (used when connection issues are detected):
```java
protected void invalidateProducer(Config config) {
    PRODUCER_CACHE.invalidate(config);
}
```

#### Abstract Methods (Must be implemented by subclasses)

```java
protected abstract Producer initProducer(Config config) throws Exception;
protected abstract void closeProducer(Producer producer) throws Exception;
```

## Usage by Subclasses

Each specific action type implements the abstract methods to handle their specific producer types:

### Example: MQTT Action
```java
@Override
protected MqttClient initProducer(IotDataSinkMqttConfig config) throws Exception {
    // Create and configure MQTT client
    String clientId = config.getClientId() + "_" + System.currentTimeMillis();
    MqttClient mqttClient = new MqttClient(config.getUrl(), clientId, new MemoryPersistence());
    mqttClient.connect(buildConnectOptions(config));
    return mqttClient;
}

@Override
protected void closeProducer(MqttClient producer) throws Exception {
    if (producer.isConnected()) {
        producer.disconnect();
    }
    producer.close();
}
```

### Example: Redis Action
```java
@Override
protected RedisTemplate<String, Object> initProducer(IotDataSinkRedisConfig config) {
    // Create and configure RedisTemplate with Redisson
    Config redissonConfig = new Config();
    // ... configuration ...
    RedissonClient redisson = Redisson.create(redissonConfig);
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(new RedissonConnectionFactory(redisson));
    // ... serializer configuration ...
    template.afterPropertiesSet();
    return template;
}

@Override
protected void closeProducer(RedisTemplate<String, Object> producer) throws Exception {
    RedisConnectionFactory factory = producer.getConnectionFactory();
    if (factory != null) {
        ((RedissonConnectionFactory) factory).destroy();
    }
}
```

## Error Handling and Logging

### Cache Operations Logging
- **Producer Creation**: INFO level when successfully created
- **Producer Creation Failure**: ERROR level with exception details
- **Producer Closure**: INFO level when successfully closed via cache eviction
- **Producer Closure Failure**: ERROR level when close operation fails

### Exception Propagation
- Exceptions during producer creation are propagated to trigger cache loading failure mechanisms
- Exceptions during producer usage are handled by individual action implementations
- Connection failures during execution trigger `invalidateProducer()` to force cache refresh

## Thread Safety

The implementation is thread-safe:
- Guava's `LoadingCache` is thread-safe for concurrent access
- The `get()` method handles concurrent creation of the same key
- Removal listeners are called sequentially for each eviction
- Subclasses must ensure their `initProducer` and `closeProducer` implementations are thread-safe

## Performance Characteristics

### Time Complexity
- Cache hit: O(1) average case
- Cache miss: O(1) for cache lookup + O(initProducer) for creation
- Producer retrieval: O(1) average case

### Space Complexity
- Proportional to the number of unique configurations in use
- Bounded by effective cache size (determined by access patterns and expiration)

### Memory Efficiency
- Only stores actively used producers
- Automatic cleanup of unused resources
- Minimal overhead per cache entry

## Integration

This base class is extended by all specific action implementations in the IoT module:
- MQTT Action (`IotMqttDataRuleAction`)
- TCP Action (`IotTcpDataRuleAction`)
- HTTP Action (`IotHttpDataSinkAction`)
- Redis Action (`IotRedisRuleAction`)
- WebSocket Action (`IotWebSocketDataRuleAction`)
- RocketMQ Action (`IotRocketMQDataRuleAction`)
- Kafka Action (`IotKafkaDataRuleAction`)
- Database Action (`IotDatabaseDataRuleAction`)
- RabbitMQ Action (`IotRabbitMQDataRuleAction`)

Each subclass provides its specific implementation of `initProducer()` and `closeProducer()` methods while inheriting the caching and resource management logic.

## Usage Guidelines

### When to Extend This Class
Extend `IotDataRuleCacheableAction` when:
1. Your action needs to maintain expensive-to-create resources (connections, clients, producers)
2. These resources can be safely reused for the same configuration
3. You want automatic cleanup of unused resources
4. You benefit from thread-safe access to these resources

### Implementation Requirements
Subclasses must implement:
1. `initProducer(Config config)`: Create and initialize the producer/resource
2. `closeProducer(Producer producer)`: Properly close/release the producer/resource

### Best Practices
1. Make producer creation idempotent where possible
2. Ensure close operations are safe to call multiple times
3. Handle configuration validation in `initProducer`
4. Log appropriately for debugging and monitoring
5. Consider connection pooling in the underlying client/library when applicable

## See Also

- [Action Module Overview](action.md)
- [MQTT Action Implementation](mqtt_action.md)
- [TCP Action Implementation](tcp_action.md)
- [HTTP Action Implementation](http_action.md)
- [Redis Action Implementation](redis_action.md)
- [WebSocket Action Implementation](websocket_action.md)
- [RocketMQ Action Implementation](rocketmq_action.md)
- [Kafka Action Implementation](kafka_action.md)
- [Database Action Implementation](database_action.md)
- [RabbitMQ Action Implementation](rabbitmq_action.md)
- [IoT Data Rule Engine](https://github.com/YunaiV/yudao-cloud/tree/master/yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/data/IotDataRuleServiceImpl.java)