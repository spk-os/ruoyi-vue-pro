# Action Module Documentation

## Overview

The Action module is part of the IoT service in the Yudao framework, responsible for executing data rule actions. When IoT rule conditions are met, these actions are triggered to send device messages to various external systems and protocols.

This module provides implementations for sending IoT device data to different endpoints including:
- Message queues (MQTT, RocketMQ, Kafka, RabbitMQ)
- Protocols (TCP, HTTP, WebSocket)
- Databases
- Caching systems (Redis)

Each action type implements the `IotDataRuleAction` interface and handles the specific protocol details for reliable message delivery.

## Architecture

The Action module follows a strategy pattern where each action type implements a common interface but handles protocol-specific logic internally. A base cacheable action class provides connection pooling and caching mechanisms to optimize performance.

Component relationships:
- IotDataRuleAction Interface (defines contract for all actions)
  - IotDataRuleCacheableAction Abstract Class (provides caching mechanism)
    - MQTT Action (IotMqttDataRuleAction)
    - TCP Action (IotTcpDataRuleAction)
    - HTTP Action (IotHttpDataSinkAction)
    - Redis Action (IotRedisRuleAction)
    - WebSocket Action (IotWebSocketDataRuleAction)
    - RocketMQ Action (IotRocketMQDataRuleAction)
    - Kafka Action (IotKafkaDataRuleAction)
    - Database Action (IotDatabaseDataRuleAction)
    - RabbitMQ Action (IotRabbitMQDataRuleAction)

## Core Components

The module consists of the following key components:

1. **IotDataRuleAction Interface** - Defines the contract for all action implementations
2. **IotDataRuleCacheableAction Abstract Class** - Provides caching mechanism for producer/connections
3. **Specific Action Implementations** - Each handles a specific protocol/system:
   - MQTT Action (`IotMqttDataRuleAction`)
   - TCP Action (`IotTcpDataRuleAction`)
   - HTTP Action (`IotHttpDataSinkAction`)
   - Redis Action (`IotRedisRuleAction`)
   - WebSocket Action (`IotWebSocketDataRuleAction`)
   - RocketMQ Action (`IotRocketMQDataRuleAction`)
   - Kafka Action (`IotKafkaDataRuleAction`)
   - Database Action (`IotDatabaseDataRuleAction`)
   - RabbitMQ Action (`IotRabbitMQDataRuleAction`)

## Sub-Modules

For detailed documentation of each action type, refer to the respective sub-module documentation:

- [MQTT Action](mqtt_action.md) - Sends data to MQTT brokers
- [TCP Action](tcp_action.md) - Sends data via TCP sockets
- [HTTP Action](http_action.md) - Sends data via HTTP/HTTPS requests
- [Redis Action](redis_action.md) - Stores data in Redis with various data structures
- [WebSocket Action](websocket_action.md) - Sends data via WebSocket connections
- [RocketMQ Action](rocketmq_action.md) - Sends data to Apache RocketMQ
- [Kafka Action](kafka_action.md) - Sends data to Apache Kafka
- [Database Action](database_action.md) - Stores data in relational databases
- [RabbitMQ Action](rabbitmq_action.md) - Sends data to RabbitMQ message broker
- [Base Action](base_action.md) - Common caching functionality for all actions

## Data Flow

When an IoT rule triggers an action, the following flow occurs:

1. Rule Engine triggers action with device message
2. Action Module gets/creates producer from cache
3. Action Module sends formatted message to External System
4. External System sends acknowledgement/response back to Action Module
5. Action Module returns action execution result to Rule Engine

## Configuration

Each action type requires specific configuration stored in the `IotDataSinkDO` entity, which includes:
- Connection details (host, port, credentials)
- Protocol-specific settings (topics, queues, etc.)
- Data format preferences (JSON, binary, etc.)
- Timeout and retry settings

The configuration is dynamically loaded from the database and cached for performance.

## Error Handling

All action implementations include comprehensive error handling:
- Connection failures trigger automatic reconnection attempts
- Message sending failures are logged and propagated
- Invalid configurations are validated at initialization
- Resources are properly cleaned up when actions are no longer needed

## Performance Considerations

- Connection pooling via Guava Cache with 30-minute expiration
- Lazy initialization of producers/connections
- Efficient serialization using JSON utilities
- Minimal object creation during message processing