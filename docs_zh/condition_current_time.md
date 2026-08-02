# Current Time Condition Matcher

## Overview

The `IotCurrentTimeConditionMatcher` is responsible for evaluating conditions based on the current system time. This matcher is used when defining rules that need to check if the current time meets certain criteria (e.g., time is within a specific range, matches a cron expression, or falls on certain days of the week).

## Responsibilities

- Evaluates time-based conditions against the current system time
- Supports various time formats and expressions (specific times, time ranges, cron expressions)
- Handles timezone considerations appropriately
- Provides detailed logging for condition matching failures
- Works independently of device messages (unlike property and state matchers)

## Key Methods

### `getSupportedConditionType()`
Returns the condition type that this matcher handles:
```java
@Override
public IotSceneRuleConditionTypeEnum getSupportedConditionType() {
    return IotSceneRuleConditionTypeEnum.CURRENT_TIME;
}
```

### `matches(IotDeviceMessage message, IotSceneRuleDO.TriggerCondition condition)`
Evaluates whether the current time satisfies the specified condition.

#### Parameters:
- `message`: The device message (note: not used in time-based evaluation but required by interface)
- `condition`: The time-based condition to evaluate

#### Returns:
- `true` if the current time satisfies the condition
- `false` otherwise

## Implementation Details

The matching process follows these steps:

1. **Basic Validation**: Checks if the condition has valid basic parameters using `IotSceneRuleMatcherHelper.isBasicConditionValid()`

2. **Product/Device Consistency Check**: Although time conditions don't depend on specific devices, this check is still performed for consistency using `IotSceneRuleMatcherHelper.productAndDeviceNotMatched()`

3. **Time Helper Utilization**: Uses `IotSceneRuleTimeHelper` to evaluate time-based expressions against the current time

4. **Result Evaluation**: Returns the result of the time evaluation

## Dependencies

- `IotSceneRuleTimeHelper`: Specialized helper for evaluating time-based conditions
- `IotSceneRuleMatcherHelper`: Helper class for common condition validation and logging
- `IotSceneRuleDO.TriggerCondition`: Data object containing condition details

## Usage Examples

### 1. Specific Time Match
Trigger at exactly 10:30 AM:
```java
IotSceneRuleDO.TriggerCondition condition = new IotSceneRuleDO.TriggerCondition();
condition.setIdentifier("10:30");     // Time to match (HH:mm format)
condition.setOperator("==");          // Equality operator
condition.setParam("");               // Not used for time equality
condition.setProductId("product123"); // Still required for consistency check
condition.setDeviceId("device456");   // Still required for consistency check
```

### 2. Time Range Match
Trigger between 9:00 AM and 5:00 PM:
```java
IotSceneRuleDO.TriggerCondition condition = new IotSceneRuleDO.TriggerCondition();
condition.setIdentifier("09:00-17:00"); // Time range (HH:mm-HH:mm format)
condition.setOperator("IN_RANGE");      // Range operator
condition.setParam("");                 // Not used
condition.setProductId("product123");
condition.setDeviceId("device456");
```

### 3. Cron Expression Match
Trigger every Monday at 9:00 AM:
```java
IotSceneRuleDO.TriggerCondition condition = new IotSceneRuleDO.TriggerCondition();
condition.setIdentifier("0 0 9 ? * MON"); // Cron expression (seconds minutes hours day month day-of-week)
condition.setOperator("CRON_MATCH");      // Cron matching operator
condition.setParam("");                   // Not used
condition.setProductId("product123");
condition.setDeviceId("device456");
```

## Implementation Details

The actual time evaluation logic is delegated to `IotSceneRuleTimeHelper`, which handles:
- Parsing various time formats (HH:mm, HH:mm-HH:mm, cron expressions)
- Comparing current time against specified times/ranges
- Evaluating cron expressions against the current timestamp
- Handling timezone conversions if necessary

## Dependencies

Unlike the property and state matchers, the current time matcher has minimal dependencies on device message content since it evaluates based on system time rather than incoming data. However, it still performs the standard product/device consistency checks for interface consistency.

## Error Handling and Logging

The matcher provides detailed logging for failed condition evaluations through `IotSceneRuleMatcherHelper.logConditionMatchFailure()`, which helps in debugging time-based rule configurations by indicating exactly why a time condition failed to match (e.g., invalid time format, time outside range, cron mismatch).

## Special Considerations

### Timezone Handling
The matcher uses the system's default timezone for time evaluations. Applications requiring specific timezone handling should ensure the server timezone is configured appropriately or modify the time helper to accept timezone parameters.

### Performance
Since this matcher doesn't depend on incoming device messages, it can be evaluated at any time, not just when messages arrive. This makes it suitable for time-triggered rules that need to execute on a schedule regardless of device activity.

### Field Usage Notes
For time conditions:
- The `identifier` field contains the time expression to match against
- The `operator` field specifies how to compare (EQUALS, IN_RANGE, CRON_MATCH, etc.)
- The `param` field is typically unused for time conditions but may be used for extended parameters in future enhancements
- The `productId` and `deviceId` fields are still required for consistency checks but don't affect the time evaluation logic

## Extension Points

To extend this matcher for additional time-based operations:
1. Add new time format parsing capabilities to `IotSceneRuleTimeHelper`
2. Extend the operator validation in `IotSceneRuleMatcherHelper` to support new time-based operators
3. Add new enum values to `IotSceneRuleConditionTypeEnum` for specialized time conditions if needed
4. The matcher's delegation to the time helper makes it relatively easy to extend with new time evaluation capabilities