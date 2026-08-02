# Redis Action Module Documentation

## Overview

The Redis Action module provides the implementation for sending IoT device messages to Redis using various Redis data structures. This allows IoT data to be stored, processed, and analyzed in Redis for real-time applications.

## Core Components

### IotRedisRuleAction

The main class implementing the `IotDataRuleAction` interface for Redis operations.

```java
@Component
@Slf4j
public class IotRedisRuleAction extends IotDataRuleCacheableAction<IotDataSinkRedisConfig, RedisTemplate<String, Object>>
```

## Supported Data Structures

The Redis Action supports multiple Redis data structures through the `IotRedisDataStructureEnum`:

| Data Structure | Type Value | Description |
|----------------|------------|-------------|
| STREAM | 1 | Redis Streams for message queuing |
| HASH | 2 | Hash maps for structured data storage |
| LIST | 3 | Lists for ordered data |
| SET | 4 | Sets for unique values |
| ZSET | 5 | Sorted sets for ranked data |
| STRING | 6 | Simple key-value storage |

## Configuration

The Redis Action requires configuration stored in `IotDataSinkRedisConfig`:

```java
public class IotDataSinkRedisConfig {
    private String host;          // Redis server host
    private Integer port;          // Redis server port
    private String password;      // Redis authentication password
    private Integer database;      // Redis database index
    private String topic;         // Redis key/topic name
    private Integer dataStructure; // Data structure type (1-6)
    private String hashField;      // Field name for HASH operations
    private String scoreField;     // Field name for ZSET score
}
```

## Usage Examples

### Stream Operations
```java
IotDataSinkRedisConfig config = new IotDataSinkRedisConfig();
config.setDataStructure(1); // STREAM
config.setTopic("device_messages");
```

### Hash Operations
```java
IotDataSinkRedisConfig config = new IotDataSinkRedisConfig();
config.setDataStructure(2); // HASH
config.setTopic("device_data");
config.setHashField("device_123"); // Optional field name
```

### ZSet Operations
```java
IotDataSinkRedisConfig config = new IotDataSinkRedisConfig();
config.setDataStructure(5); // ZSET
config.setTopic("device_timestamps");
config.setScoreField("timestamp"); // Field containing score value
```

## Implementation Details

### Connection Management

The action uses Redisson for Redis connection management:

```java
@Override
protected RedisTemplate<String, Object> initProducer(IotDataSinkRedisConfig config) {
    // 1. Create Redisson configuration
    Config redissonConfig = new Config();
    SingleServerConfig serverConfig = redissonConfig.useSingleServer()
            .setAddress("redis://" + config.getHost() + ":" + config.getPort())
            .setDatabase(config.getDatabase());
    
    // 2. Set password if configured
    if (StrUtil.isNotBlank(config.getPassword())) {
        serverConfig.setPassword(config.getPassword());
    }
    
    // 3. Create RedisTemplate with Redisson connection
    RedissonClient redisson = Redisson.create(redissonConfig);
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(new RedissonConnectionFactory(redisson));
    
    // 4. Configure serializers
    template.setKeySerializer(RedisSerializer.string());
    template.setHashKeySerializer(RedisSerializer.string());
    template.setValueSerializer(RedisSerializer.json());
    template.setHashValueSerializer(RedisSerializer.json());
    template.afterPropertiesSet();
    
    return template;
}
```

### Message Processing

Each data structure type has a dedicated execution method:

```java
@Override
public void execute(IotDeviceMessage message, IotDataSinkRedisConfig config) throws Exception {
    // 1. Get RedisTemplate from cache
    RedisTemplate<String, Object> redisTemplate = getProducer(config);
    
    // 2. Convert message to JSON
    String messageJson = JsonUtils.toJsonString(message);
    
    // 3. Execute based on data structure type
    IotRedisDataStructureEnum dataStructure = getDataStructureByType(config.getDataStructure());
    switch (dataStructure) {
        case STREAM: executeStream(redisTemplate, config, messageJson); break;
        case HASH: executeHash(redisTemplate, config, message, messageJson); break;
        case LIST: executeList(redisTemplate, config, messageJson); break;
        case SET: executeSet(redisTemplate, config, messageJson); break;
        case ZSET: executeZSet(redisTemplate, config, message, messageJson); break;
        case STRING: executeString(redisTemplate, config, messageJson); break;
    }
    
    log.info("[execute][消息发送成功] dataStructure: {}, config: {}", dataStructure.getName(), config);
}
```

### Data Structure Operations

Each data structure type has its own execution method:

#### Stream Operations
```java
private void executeStream(RedisTemplate<String, Object> redisTemplate, 
                          IotDataSinkRedisConfig config, String messageJson) {
    ObjectRecord<String, ?> record = StreamRecords.newRecord()
            .ofObject(messageJson).withStreamKey(config.getTopic());
    redisTemplate.opsForStream().add(record);
}
```

#### Hash Operations
```java
private void executeHash(RedisTemplate<String, Object> redisTemplate, 
                        IotDataSinkRedisConfig config, IotDeviceMessage message, String messageJson) {
    String hashField = StrUtil.isNotBlank(config.getHashField()) ?
            config.getHashField() : String.valueOf(message.getDeviceId());
    redisTemplate.opsForHash().put(config.getTopic(), hashField, messageJson);
}
```

#### ZSet Operations
```java
private void executeZSet(RedisTemplate<String, Object> redisTemplate, 
                        IotDataSinkRedisConfig config, IotDeviceMessage message, String messageJson) {
    double score;
    if (StrUtil.isNotBlank(config.getScoreField())) {
        // Try to get score from message
        try {
            Map<String, Object> messageMap = JsonUtils.parseObject(messageJson, Map.class);
            Object scoreValue = messageMap.get(config.getScoreField());
            score = scoreValue instanceof Number ? ((Number) scoreValue).doubleValue() : System.currentTimeMillis();
        } catch (Exception e) {
            score = System.currentTimeMillis();
        }
    } else {
        score = System.currentTimeMillis();
    }
    redisTemplate.opsForZSet().add(config.getTopic(), messageJson, score);
}
```

## Error Handling

The Redis Action includes comprehensive error handling:

```java
try {
    // Message processing logic
    log.info("[execute][消息发送成功] dataStructure: {}, config: {}", dataStructure.getName(), config);
} catch (Exception e) {
    log.error("[execute][消息发送失败] dataStructure: {}, config: {}", dataStructure.getName(), config, e);
    throw e;
}
```

## Performance Considerations

- Uses Redisson for efficient Redis connection management
- JSON serialization for message data
- Connection pooling via Guava Cache with 30-minute expiration
- Minimal object creation during message processing

## Integration

The Redis Action integrates with the IoT rule engine through the `IotDataRuleAction` interface. When a rule condition is met, the rule engine calls the appropriate action implementation based on the configured sink type.

## See Also

- [Action Module Overview](action.md)
- [IoT Data Rule Engine](https://github.com/YunaiV/yudao-cloud/tree/master/yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/data/IotDataRuleServiceImpl.java)
- [Redis Streams Documentation](https://redis.io/docs/data-types/streams/)
- [Redisson Documentation](https://github.com/redisson/redisson/wiki)