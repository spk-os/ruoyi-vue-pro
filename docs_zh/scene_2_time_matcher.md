# scene_2_time_matcher.md

# 时间匹配工具 (Time Matcher)

## 概述

时间匹配工具是 scene_2 模块中的一个核心组件，提供时间条件匹配的通用方法。该工具类被 `IotCurrentTimeConditionMatcher` 和 `IotTimerConditionEvaluator` 共同使用，用于评估各种时间相关的条件表达式。

## 核心功能

### 1. 时间操作符判断

#### isDateTimeOperator
判断是否为日期时间操作符（基于时间戳的操作）：
- DATE_TIME_GREATER_THAN
- DATE_TIME_LESS_THAN
- DATE_TIME_BETWEEN

#### isTimeOperator
判断是否为时间操作符（包括日期时间操作符和当日时间操作符）：
- TIME_GREATER_THAN
- TIME_LESS_THAN
- TIME_BETWEEN
- 日期时间操作符

### 2. 时间匹配执行

#### executeTimeMatching
执行时间匹配逻辑的主入口方法：
- 根据操作符类型决定使用日期时间匹配还是当日时间匹配
- 日期时间匹配：基于时间戳（秒级）进行比较
- 当日时间匹配：基于 HH:mm:ss 格式进行比较

### 3. 日期时间匹配

#### matchDateTime
匹配单个日期时间戳：
- 支持 GREATER_THAN 和 LESS_THAN 操作
- 将参数解析为时间戳并与当前时间戳比较

#### matchDateTimeBetween
匹配日期时间区间：
- 参数格式：startTimestamp,endTimestamp
- 检查当前时间戳是否在指定区间内（包含边界）

### 4. 当日时间匹配

#### matchTime
匹配单个当日时间：
- 支持 GREATER_THAN 和 LESS_THAN 操作
- 解析时间字符串并与当前时间比较

#### matchTimeBetween
匹配当日时间区间：
- 参数格式：startTime,endTime
- 检查当前时间是否在指定时间区间内（包含边界）

### 5. 时间解析

#### parseTime
解析时间字符串：
- 支持 HH:mm 格式（5个字符）
- 支持 HH:mm:ss 格式（8个字符）
- 自动识别格式并进行解析
- 提供详细的错误日志和异常处理

## 技术实现

### 依赖
- cn.hutool.core.lang.Assert
- cn.hutool.core.text.CharPool
- cn.hutool.core.util.StrUtil
- cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils
- cn.iocoder.yudao.module.iot.enums.rule.IotSceneRuleConditionOperatorEnum
- lombok.experimental.UtilityClass
- lombok.extern.slf4j.Slf4j
- java.time.LocalDateTime
- java.time.LocalTime
- java.time.format.DateTimeFormatter
- java.util.List

### 关键实现细节

1. **时间格式化器**：
   - TIME_FORMATTER: HH:mm:ss 格式
   - TIME_FORMATTER_SHORT: HH:mm 格式

2. **异常处理**：
   - 所有匹配方法都包含try-catch块
   - 出现异常时记录错误日志并返回false
   - 时间解析方法会抛出IllegalArgumentException

3. **性能考虑**：
   - 使用静态最终变量存储格式化器
   - 避免在热路径中创建对象
   - 使用Hutool工具类进行字符串处理

## 使用示例

### 判断操作符类型
```java
// 判断是否为日期时间操作符
boolean isDateTimeOp = IotSceneRuleTimeHelper.isDateTimeOperator(
    IotSceneRuleConditionOperatorEnum.DATE_TIME_GREATER_THAN);

// 判断是否为时间操作符
boolean isTimeOp = IotSceneRuleTimeHelper.isTimeOperator(
    IotSceneRuleConditionOperatorEnum.TIME_GREATER_THAN);
```

### 执行时间匹配
```java
// 日期时间匹配（时间戳）
boolean result1 = IotSceneRuleTimeHelper.executeTimeMatching(
    IotSceneRuleConditionOperatorEnum.DATE_TIME_GREATER_THAN, 
    "1640995200"); // 2022-01-01 00:00:00 时间戳

// 当日时间匹配
boolean result2 = IotSceneRuleTimeHelper.executeTimeMatching(
    IotSceneRuleConditionOperatorEnum.TIME_GREATER_THAN, 
    "09:00:00"); // 早上9点
```

### 时间区间匹配
```java
// 日期时间区间匹配
boolean result3 = IotSceneRuleTimeHelper.matchDateTimeBetween(
    System.currentTimeMillis() / 1000, 
    "1640995200,1641081600"); // 2022-01-01 00:00:00 到 2022-01-02 00:00:00

// 当日时间区间匹配
boolean result4 = IotSceneRuleTimeHelper.matchTimeBetween(
    LocalTime.now(), 
    "09:00:00,17:00:00"); // 早上9点到下午5点
```

## 与其他组件的交互

时间匹配工具主要被以下组件使用：
1. IotCurrentTimeConditionMatcher - 用于匹配基于当前时间的条件
2. IotTimerConditionEvaluator - 用于评估定时触发器的条件

这些组件通过调用工具类的静态方法来执行实际的时间匹配逻辑，实现了代码的复用和解耦。

## 注意事项

1. 时间戳使用秒级精度，需要确保传入的参数也是秒级时间戳
2. 时间字符串必须严格按照 HH:mm 或 HH:mm:ss 格式
3. 区间匹配包含边界值（即 >= start 且 <= end）
4. 所有方法都是线程安全的，因为它们不依赖于可变状态