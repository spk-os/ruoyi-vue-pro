# Condition Module Documentation

## Overview

The Condition Module is part of the IoT service rule engine in the Yudao platform. It provides a flexible and extensible mechanism for evaluating conditions that determine when IoT automation rules should be triggered. The module implements a matcher pattern where different types of conditions are handled by specialized matcher components.

This module is responsible for evaluating three main types of conditions:
1. **Device Property Conditions** - Evaluates specific property values reported by devices
2. **Device State Conditions** - Evaluates the overall state/status of devices
3. **Current Time Conditions** - Evaluates conditions based on the system time

These matchers work together with the rule engine to enable sophisticated automation scenarios based on device data, device status, and time-based triggers.

## Architecture

The condition module follows a strategy pattern where each condition type has its own matcher implementation. All matchers implement the `IotSceneRuleConditionMatcher` interface, which defines the contract for condition evaluation.

### Component Structure

```
Condition Module
├── IotDevicePropertyConditionMatcher   (Handles device property conditions)
├── IotDeviceStateConditionMatcher      (Handles device state conditions)
└── IotCurrentTimeConditionMatcher      (Handles time-based conditions)
```

Each matcher is responsible for:
- Validating condition parameters
- Extracting relevant data from device messages (where applicable)
- Evaluating the condition against the extracted data
- Providing detailed logging for debugging purposes

## How It Works

When a device message is received by the IoT platform, the rule engine evaluates all active rules to determine if any should be triggered. For each rule, the engine:

1. Retrieves the trigger conditions associated with the rule
2. For each condition, finds the appropriate matcher based on the condition type
3. Calls the matcher's `matches()` method with the device message and condition details
4. If all conditions in a rule evaluate to true, the rule's actions are executed

## Sub-Modules

The condition module consists of three specialized matchers, each documented in detail in their respective files:

### 1. Device Property Condition Matcher
[Details](condition_device_property.md)

Handles conditions based on specific property values reported by IoT devices. Examples:
- Temperature > 30°C
- Humidity < 30%
- Battery level <= 20%
- Switch state == "on"

### 2. Device State Condition Matcher
[Details](condition_device_state.md)

Handles conditions based on the overall state or status of devices. Examples:
- Device status == "online"
- Device status == "offline"
- Connection status == "connected"
- Device mode == "auto"

### 3. Current Time Condition Matcher
[Details](condition_current_time.md)

Handles conditions based on the current system time. Examples:
- Time == "09:00" (at 9 AM)
- Time IN_RANGE "08:00-18:00" (during business hours)
- Time CRON_MATCH "0 0 22 ? * MON-FRI" (weekdays at 10 PM)

## Extension Mechanism

To add new condition types to the system:

1. Create a new enum value in `IotSceneRuleConditionTypeEnum` for the new condition type
2. Implement the `IotSceneRuleConditionMatcher` interface for your new condition type
3. Register your implementation as a Spring `@Component`
4. The rule engine will automatically discover and use your matcher based on the condition type

## Data Flow

```
Device Message Received
        ↓
Rule Engine Evaluates Rules
        ↓
For Each Rule:
    For Each Condition:
        ↓
    Find Matcher by Condition Type
        ↓
    Call matcher.matches(message, condition)
        ↓
    If All Conditions Match:
        ↓
    Execute Rule Actions
```

## Configuration

All condition matchers are automatically registered as Spring components via the `@Component` annotation. No additional configuration is required for basic usage.

For advanced configuration of matching behavior (such as custom time zones for time-based conditions), modify the respective helper classes:
- `IotSceneRuleMatcherHelper` - Common matching utilities
- `IotSceneRuleTimeHelper` - Time-specific evaluation logic

## Dependencies

The condition module depends on several core IoT modules:
- `iot-core`: For device message utilities and constants
- `iot-service-rule`: For the matcher interface and rule data structures
- `iot-util`: For various utility functions used in condition evaluation

## Usage in Rules

Conditions are defined in the `IotSceneRuleDO.TriggerCondition` object, which contains:
- `conditionType`: The type of condition (matches matcher implementations)
- `identifier`: What to check (property name, state indicator, time expression)
- `operator`: How to compare (==, !=, >, <, IN_RANGE, etc.)
- `param`: The value to compare against
- `productId`: Expected product ID for validation
- `deviceId`: Expected device ID for validation

## Testing

Each matcher includes comprehensive logging to facilitate testing and debugging:
- Successful matches are logged at DEBUG level
- Failed matches are logged at DEBUG level with specific failure reasons
- This allows administrators to troubleshoot why certain rules are not triggering as expected

## Performance Considerations

The condition matchers are designed to be lightweight and efficient:
- Minimal object creation during evaluation
- Early termination when validation fails
- Efficient data extraction from device messages
- Stateless design allowing for safe concurrent usage

## Integration Points

The condition module is primarily used by:
- Rule evaluation engine in the IoT service
- Rule testing and validation tools
- Administrative interfaces for rule creation and management

## Related Modules

- [IoT Core Module](../iot_core.md) - Contains device message utilities used by the matchers
- [IoT Rule Service Module](../iot_rule_service.md) - Contains the rule engine that uses these matchers
- [IoT Util Module](../iot_util.md) - Contains shared utilities

For more information on how conditions fit into the overall rule engine architecture, please refer to the IoT Rule Service documentation.