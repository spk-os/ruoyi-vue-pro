# matcher_interface 模块文档

## 模块概述

`matcher_interface` 模块是物联网（IoT）模块中场景规则引擎的核心组件，位于 `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/` 路径下。该模块定义了场景规则匹配器的基础接口及其实现，用于判断设备消息是否满足特定的触发条件或场景条件。

该模块采用策略模式设计，通过不同的匹配器实现来处理各种类型的设备事件和条件，使得场景规则引擎具有良好的可扩展性和灵活性。

## 核心组件

### 主接口

#### IotSceneRuleMatcher

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/IotSceneRuleMatcher.java`

IoT场景规则匹配器的基础接口，定义了所有匹配器的通用行为。

```java
public interface IotSceneRuleMatcher {

    /**
     * 获取匹配优先级（数值越小优先级越高）
     * <p>
     * 用于在多个匹配器支持同一类型时确定优先级
     *
     * @return 优先级数值
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否启用该匹配器
     * <p>
     * 可用于动态开关某些匹配器
     *
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}
```

**职责**：
- 定义所有匹配器的通契约
- 提供默认的优先级实现（默认优先级为100）
- 提供默认的启用状态实现（默认启用）

### 辅助类

#### IotSceneRuleMatcherHelper

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/IotSceneRuleMatcherHelper.java`

提供匹配器通用功能的工具类，包含条件评估、参数验证、日志记录等辅助方法。

**主要功能**：
1. **条件评估**：通过Spring表达式语言(SpEL)评估各种比较操作
2. **参数验证**：检查触发器和条件的基本参数有效性
3. **标识符匹配**：验证设备ID和产品ID的一致性
4. **日志记录**：记录匹配成功和失败的详细信息
5. **设备验证**：验证消息中的设备和产品信息与规则配置的一致性

**关键方法**：
- `evaluateCondition(Object sourceValue, String operator, String paramValue)`：评估条件是否匹配
- `isBasicTriggerValid(IotSceneRuleDO.Trigger trigger)`：验证触发器基本参数
- `isBasicConditionValid(IotSceneRuleDO.TriggerCondition condition)`：验证条件基本参数
- `isIdentifierMatched(String expectedIdentifier, String actualIdentifier)`：检查标识符是否匹配
- `productAndDeviceNotMatched(IotDeviceMessage message, Long productId, Long deviceId)`：验证产品和设备一致性
- 各种日志记录方法（如`logTriggerMatchSuccess`、`logConditionMatchFailure`等）

### 触发器匹配器实现

触发器匹配器用于判断何时应该触发场景规则的执行。

#### IotDevicePropertyPostTriggerMatcher

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/trigger/IotDevicePropertyPostTriggerMatcher.java`

处理设备属性上报（PROPERTY_POST）类型的触发器。

**支持的触发器类型**：`IotSceneRuleTriggerTypeEnum.DEVICE_PROPERTY_POST`

**匹配逻辑**：
1. 验证触发器基本参数有效性
2. 检查消息方法是否为属性上报（PROPERTY_POST）
3. 验证产品和设备ID一致性
4. 检查消息中是否包含指定的属性标识符
5. 验证操作符和值的有效性
6. 提取属性值并使用条件评估器进行匹配
7. 记录匹配结果日志

**优先级**：20（中等偏高）

#### IotDeviceEventPostTriggerMatcher

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/trigger/IotDeviceEventPostTriggerMatcher.java`

处理设备事件上报（EVENT_POST）类型的触发器。

**支持的触发器类型**：`IotSceneRuleTriggerTypeEnum.DEVICE_EVENT_POST`

**匹配逻辑**：
1. 验证触发器基本参数有效性
2. 检查消息方法是否为事件上报（EVENT_POST）
3. 验证产品和设备ID一致性
4. 检查标识符是否匹配（事件类型）
5. 如果配置了操作符和值，则提取事件值并进行条件匹配
6. 记录匹配结果日志

**优先级**：30（中等）

#### IotTimerTriggerMatcher

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/trigger/IotTimerTriggerMatcher.java`

处理定时器（TIMER）类型的触发器。

**支持的触发器类型**：`IotSceneRuleTriggerTypeEnum.TIMER`

**匹配逻辑**：
1. 验证触发器基本参数有效性
2. 检查CRON表达式是否存在且格式有效
3. 定时触发器不依赖具体的设备消息，主要验证配置有效性
4. 记录匹配结果日志

**优先级**：50（最低，因为定时触发器不依赖消息）

#### IotDeviceServiceInvokeTriggerMatcher

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/trigger/IotDeviceServiceInvokeTriggerMatcher.java`

处理设备服务调用（SERVICE_INVOKE）类型的触发器。

**支持的触发器类型**：`IotSceneRuleTriggerTypeEnum.DEVICE_SERVICE_INVOKE`

**匹配逻辑**：
1. 验证触发器基本参数有效性
2. 检查消息方法是否为服务调用（SERVICE_INVOKE）
3. 验证产品和设备ID一致性
4. 检查标识符是否匹配（服务ID）
5. 如果配置了参数条件，则提取服务输入参数并进行匹配
6. 记录匹配结果日志

**优先级**：40（较低）

#### IotDeviceStateUpdateTriggerMatcher

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/trigger/IotDeviceStateUpdateTriggerMatcher.java`

处理设备状态更新（STATE_UPDATE）类型的触发器。

**支持的触发器类型**：`IotSceneRuleTriggerTypeEnum.DEVICE_STATE_UPDATE`

**匹配逻辑**：
1. 验证触发器基本参数有效性
2. 检查消息方法是否为状态更新（STATE_UPDATE）
3. 验证产品和设备ID一致性
4. 验证操作符和值的有效性
5. 提取状态值并使用条件评估器进行匹配
6. 记录匹配结果日志

**优先级**：10（最高）

### 条件匹配器实现

条件匹配器用于判断场景规则的条件是否满足。

#### IotDevicePropertyConditionMatcher

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/condition/IotDevicePropertyConditionMatcher.java`

处理设备属性（DEVICE_PROPERTY）类型的条件。

**支持的条件类型**：`IotSceneRuleConditionTypeEnum.DEVICE_PROPERTY`

**匹配逻辑**：
1. 验证条件基本参数有效性
2. 验证产品和设备ID一致性
3. 检查消息中是否包含指定的属性标识符
4. 验证操作符和参数的有效性
5. 提取属性值并使用条件评估器进行匹配
6. 记录匹配结果日志

**优先级**：25（中等偏高）

#### IotDeviceStateConditionMatcher

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/condition/IotDeviceStateConditionMatcher.java`

处理设备状态（DEVICE_STATE）类型的条件。

**支持的条件类型**：`IotSceneRuleConditionTypeEnum.DEVICE_STATE`

**匹配逻辑**：
1. 验证条件基本参数有效性
2. 验证产品和设备ID一致性
3. 验证操作符和参数的有效性
4. 获取设备状态值（从消息的params.state字段）
5. 使用条件评估器进行匹配
6. 记录匹配结果日志

**优先级**：30（中等）

#### IotCurrentTimeConditionMatcher

位置: `yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/service/rule/scene/matcher/condition/IotCurrentTimeConditionMatcher.java`

处理当前时间（CURRENT_TIME）类型的条件。

**支持的条件类型**：`IotSceneRuleConditionTypeEnum.CURRENT_TIME`

**匹配逻辑**：
1. 验证条件基本参数有效性
2. 验证产品和设备ID一致性
3. 验证操作符和参数的有效性
4. 验证操作符是否为支持的时间操作符
5. 使用时间帮助类执行时间匹配
6. 记录匹配结果日志

**优先级**：40（较低）

## 架构设计

### 类关系图

### 类关系图

以下是matcher_interface模块的类关系图，展示了各个接口和实现类之间的关系：

1. **IotSceneRuleMatcher** 是所有匹配器的基础接口，定义了getPriority()和isEnabled()方法
2. **IotSceneRuleTriggerMatcher** 和 **IotSceneRuleConditionMatcher** 是IotSceneRuleMatcher的子接口
3. 各种具体的触发器匹配器（如IotDevicePropertyPostTriggerMatcher）实现IotSceneRuleTriggerMatcher接口
4. 各种具体的条件匹配器（如IotDevicePropertyConditionMatcher）实现IotSceneRuleConditionMatcher接口
5. **IotSceneRuleMatcherHelper** 是辅助工具类，被所有匹配器使用

```
classDiagram
    IotSceneRuleMatcher <|-- IotSceneRuleTriggerMatcher
    IotSceneRuleMatcher <|-- IotSceneRuleConditionMatcher
    
    IotSceneRuleTriggerMatcher <|.. IotDevicePropertyPostTriggerMatcher
    IotSceneRuleTriggerMatcher <|.. IotDeviceEventPostTriggerMatcher
    IotSceneRuleTriggerMatcher <|.. IotTimerTriggerMatcher
    IotSceneRuleTriggerMatcher <|.. IotDeviceServiceInvokeTriggerMatcher
    IotSceneRuleTriggerMatcher <|.. IotDeviceStateUpdateTriggerMatcher
    
    IotSceneRuleConditionMatcher <|.. IotDevicePropertyConditionMatcher
    IotSceneRuleConditionMatcher <|.. IotDeviceStateConditionMatcher
    IotSceneRuleConditionMatcher <|.. IotCurrentTimeConditionMatcher
    
    IotSceneRuleMatcherHelper --> IotSceneRuleMatcher : 使用
```

### 工作流程

场景规则匹配过程遵循以下步骤：

### 工作流程

场景规则匹配过程遵循以下步骤：

1. 接收设备消息
2. 遍历所有匹配器
3. 检查匹配器是否启用
   - 如果未启用，跳过此匹配器
   - 如果已启用，继续下一步
4. 检查匹配器类型是否匹配
   - 如果不匹配，跳过此匹配器
   - 如果匹配，继续下一步
5. 调用matches()方法进行匹配
6. 检查匹配结果
   - 如果匹配成功，触发对应的规则动作
   - 如果匹配失败，继续检查其他匹配器
7. 结束处理

以下是流程的文本描述：

```
[接收设备消息] --> [遍历所有匹配器] --> {匹配器是否启用？}
    -->|否| [跳过此匹配器]
    -->|是| {匹配器类型是否匹配？}
        -->|否| [跳过此匹配器]
        -->|是| [调用matches()方法]
            --> {匹配结果}
                -->|是| [触发对应的规则动作]
                -->|否| [继续检查其他匹配器]
[触发对应的规则动作] --> [结束处理]
```

### 优先级机制

匹配器通过`getPriority()`方法返回优先级值，数值越小表示优先级越高。系统在匹配时会按照优先级从高到低的顺序检查匹配器，一旦找到匹配的匹配器，即停止后续匹配（虽然当前实现中所有匹配器都会被检查，但日志记录和处理会考虑优先级）。

当前匹配器优先级分配：
- IotDeviceStateUpdateTriggerMatcher: 10 (最高优先级)
- IotDevicePropertyPostTriggerMatcher: 20
- IotDevicePropertyConditionMatcher: 25
- IotDeviceEventPostTriggerMatcher: 30
- IotDeviceStateConditionMatcher: 30
- IotDeviceServiceInvokeTriggerMatcher: 40
- IotCurrentTimeConditionMatcher: 40
- IotTimerTriggerMatcher: 50 (最低优先级)

## 详细实现说明

### 条件评估机制

所有条件匹配器最终都依赖于`IotSceneRuleMatcherHelper.evaluateCondition()`方法来进行实际的值比较。该方法使用Spring表达式语言(SpEL)来支持各种比较操作。

支持的操作符包括：
- 等于 (`=`)
- 不等于 (`!=`)
- 大于 (`>`)
- 小于 (`<`)
- 大于等于 (`>=`)
- 小于等于 (`<=`)
- 在范围内 (`BETWEEN`)
- 不在范围内 (`NOT_BETWEEN`)
- 包含 (`CONTAINS`)
- 不包含 (`NOT_CONTAINS`)
- 以...开头 (`STARTS_WITH`)
- 以...结尾 (`ENDS_WITH`)
- 正则匹配 (`REGEX`)

对于数值比较，系统会自动将值转换为数字类型以确保比较的准确性。

### 设备和产品验证

为了确保规则只应用于正确的设备和产品，所有匹配器在处理前都会调用`IotSceneRuleMatcherHelper.productAndDeviceNotMatched()`方法来验证：
1. 消息中的设备ID是否与规则配置的设备ID匹配（考虑特殊值`DEVICE_ID_ALL`表示所有设备）
2. 消息中的产品ID是否与规则配置的产品ID匹配

### 错误处理和日志

所有匹配器实现都遵循一致的错误处理模式：
1. 按顺序执行各种验证步骤
2. 任何验证失败时立即返回false并记录详细的失败日志
3. 所有验证通过后执行核心匹配逻辑
4. 根据匹配结果记录成功或失败的日志

日志信息包括：
- 消息ID（如果可用）
- 触发器/条件类型
- 失败原因（如适用）

这种详细的日志记录有助于调试和监控场景规则的执行情况。

## 使用示例

### 自定义触发器匹配器

如果需要添加新的触发器类型，可以按照以下步骤实现：

1. 创建新的触发器类型枚举值（在`IotSceneRuleTriggerTypeEnum`中）
2. 实现`IotSceneRuleTriggerMatcher`接口
3. 在`getSupportedTriggerType()`方法中返回新的触发器类型
4. 实现`matches()`方法按照特定逻辑处理消息
5. 根据业务需求设置适当的优先级
6. 将实现类标注为`@Component`以便Spring自动检测

```java
@Component
public class MyCustomTriggerMatcher implements IotSceneRuleTriggerMatcher {

    @Override
    public IotSceneRuleTriggerTypeEnum getSupportedTriggerType() {
        return IotSceneRuleTriggerTypeEnum.MY_CUSTOM_TYPE; // 需要先在枚举中定义
    }

    @Override
    public boolean matches(IotDeviceMessage message, IotSceneRuleDO.Trigger trigger) {
        // 实现特定的匹配逻辑
        // 1. 基础验证
        if (!IotSceneRuleMatcherHelper.isBasicTriggerValid(trigger)) {
            IotSceneRuleMatcherHelper.logTriggerMatchFailure(message, trigger, "触发器基础参数无效");
            return false;
        }
        
        // 2. 特定验证逻辑
        // ...
        
        // 3. 核心匹配逻辑
        // ...
        
        // 4. 记录结果
        if (matched) {
            IotSceneRuleMatcherHelper.logTriggerMatchSuccess(message, trigger);
        } else {
            IotSceneRuleMatcherHelper.logTriggerMatchFailure(message, trigger, "自定义匹配失败原因");
        }
        return matched;
    }

    @Override
    public int getPriority() {
        return 15; // 根据业务需求设置优先级
    }
}
```

### 自定义条件匹配器

添加新的条件类型同样遵循类似模式：

1. 创建新的条件类型枚举值（在`IotSceneRuleConditionTypeEnum`中）
2. 实现`IotSceneRuleConditionMatcher`接口
3. 在`getSupportedConditionType()`方法中返回新的条件类型
4. 实现`matches()`方法按照特定逻辑处理条件
5. 根据业务需求设置适当的优先级
6. 将实现类标注为`@Component`

## 配置和扩展点

### Spring配置

所有匹配器实现都使用了`@Component`注解，因此会被Spring容器自动扫描和注册。系统通过依赖注入的方式获取所有可用的匹配器实现。

### 扩展建议

1. **添加新操作符**：如果需要支持额外的比较操作符，可以扩展`IotSceneRuleConditionOperatorEnum`并更新`IotSceneRuleMatcherHelper`中的相应逻辑。

2. **自定义消息处理**：对于特殊类型的设备消息，可以增强`IotDeviceMessageUtils`类中的解析方法。

3. **性能优化**：对于高频触发的场景，可以考虑在匹配器中添加缓存机制，特别是对于不经常变化的配置数据。

4. **异步处理**：对于耗时较长的匹配操作，可以考虑将匹配逻辑改为异步执行，以避免阻塞消息处理线程。

## 与其他模块的关系

### 依赖的模块

1. **iot-service**：依赖设备服务（`IotDeviceService`）来获取设备信息和缓存
2. **iot-core**：依赖消息工具类（`IotDeviceMessageUtils`）和产品认证工具（`IotProductAuthUtils`）
3. **infra-framework**：依赖Spring表达式工具（`SpringExpressionUtils`）和Spring工具（`SpringUtils`）
4. **util-framework**：依赖各种工具类如`StringUtil`、`NumberUtil`、`ObjectUtil`等

### 被依赖的模块

1. **iot-service**：场景规则服务（`IotSceneRuleServiceImpl`）使用匹配器来评估规则触发条件
2. **iot-job**：定时任务可能会触发基于时间的规则评估

## 性能考虑

1. **早期退出**：匹配器在验证过程中尽可能早地返回false，以避免不必要的计算
2. **条件评估优化**：在`IotSceneRuleMatcherHelper`中，对于数值比较会进行特殊处理以避免SpEL中的比较问题
3. **日志级别**：匹配过程中的日志记录使用DEBUG级别，在生产环境中不会产生显著的性能影响
4. **Spring管理**：所有匹配器作为Spring Bean被管理，避免了重复实例化的开销

## 最佳实践

1. **单一职责原则**：每个匹配器只负责一种类型的触发器或条件的匹配逻辑
2. **统一错误处理**：所有匹配器遵循相同的验证和错误报告模式
3. **可配置优先级**：通过调整优先级值可以控制匹配器的执行顺序
4. **详细日志**：所有匹配过程都有详细的日志记录，便于问题排查
5. **解耦设计**：匹配器之间通过接口解耦，易于扩展和维护
6. **异常安全**：所有方法都包含适当的异常处理，确保异常情况下不会导致系统崩溃

## 测试考虑点

1. **边界值测试**：测试各种操作符的边界条件（如等于、不等于、大于等于等）
2. **空值处理**：验证当消息中缺少必要字段时的行为
3. **类型转换**：测试不同数据类型之间的比较（字符串、数字、布尔值）
4. **产品/设备验证**：测试产品ID和设备ID不匹配时的正确处理
5. **优先级验证**：确保高优先级的匹配器确实在低优先级之前被调用
6. **异常情况**：测试各种异常输入（如无效的操作符、格式错误的CRON表达式等）

## 结论

`matcher_interface` 模块为物联网平台的场景规则引擎提供了一个灵活、可扩展且易于维护的匹配器框架。通过清晰的接口设计、统一的实现模式以及强大的辅助工具类，该模块使得添加新的触发器和条件类型变得简单直接，同时保持了高内聚低耦合的设计原则。

该模块的设计充分考虑了实际生产环境中的需求，包括性能优化、错误处理、日志记录和可扩展性，使其能够有效地支持复杂的物联网场景自动化需求。