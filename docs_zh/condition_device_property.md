# Device Property Condition Matcher

## Overview

The `IotDevicePropertyConditionMatcher` is responsible for evaluating conditions based on device property values reported in device messages. This matcher is used when defining rules that need to check if a specific device property meets certain criteria (e.g., temperature > 30, humidity < 60).

## Responsibilities

- Validates that the device message contains the specified property identifier
- Extracts the property value from the device message
- Evaluates the property value against the condition using the specified operator and parameters
- Handles various data types and comparison operations
- Provides detailed logging for condition matching failures

## Key Methods

### `getSupportedConditionType()`
Returns the condition type that this matcher handles:
```java
@Override
public IotSceneRuleConditionTypeEnum getSupportedConditionType() {
    return IotSceneRuleConditionTypeEnum.DEVICE_PROPERTY;
}
```

### `matches(IotDeviceMessage message, IotSceneRuleDO.TriggerCondition condition)`
Evaluates whether the given device message satisfies the specified condition.

#### Parameters:
- `message`: The device message containing property values
- `condition`: The condition to evaluate against the message

#### Returns:
- `true` if the condition is satisfied by the message
- `false` otherwise

## Implementation Details

The matching process follows these steps:

1. **Basic Validation**: Checks if the condition has valid basic parameters using `IotSceneRuleMatcherHelper.isBasicConditionValid()`

2. **Product/Device Consistency Check**: Verifies that the message corresponds to the correct product and device using `IotSceneRuleMatcherHelper.productAndDeviceNotMatched()`

3. **Property Existence Check**: Ensures the message contains the specified property identifier using `IotDeviceMessageUtils.notContainsIdentifier()`

4. **Operator/Parameter Validation**: Validates that the condition has a valid operator and parameters using `IotSceneRuleMatcherHelper.isConditionOperatorAndParamValid()`

5. **Value Extraction and Comparison**: 
   - Extracts the property value from the message
   - Converts values to appropriate types for comparison
   - Applies the specified operator (equals, greater than, less than, etc.)
   - Returns the comparison result

## Dependencies

- `IotDeviceMessageUtils`: Utility for extracting and checking device message properties
- `IotSceneRuleMatcherHelper`: Helper class for common condition validation and logging
- `IotSceneRuleDO.TriggerCondition`: Data object containing condition details

## Usage Example

Consider a rule that triggers when a temperature sensor reports a value greater than 30°C:

```java
// Condition configuration
IotSceneRuleDO.TriggerCondition condition = new IotSceneRuleDO.TriggerCondition();
condition.setIdentifier("temperature"); // Property to check
condition.setOperator(">");             // Operator
condition.setParam("30");               // Threshold value
condition.setProductId("product123");   // Expected product ID
condition.setDeviceId("device456");     // Expected device ID

// When a device message arrives:
IotDeviceMessage message = new IotDeviceMessage();
message.setProductId("product123");
message.setDeviceId("device456");
message.putParam("temperature", 35.0);  // Current temperature reading

// Evaluation:
boolean matches = devicePropertyConditionMatcher.matches(message, condition);
// Returns true since 35.0 > 30
```

## Error Handling and Logging

The matcher provides detailed logging for failed condition evaluations through `IotSceneRuleMatcherHelper.logConditionMatchFailure()`, which helps in debugging rule configurations by indicating exactly why a condition failed to match (e.g., missing property, invalid operator, value mismatch).

## Extension Points

To extend this matcher for additional property types or comparison operations:
1. Modify the value extraction logic in `IotDeviceMessageUtils` if new data types need special handling
2. Extend the operator validation in `IotSceneRuleMatcherHelper` to support new operators
3. The core matching logic is designed to be extensible for various data types and comparison operations