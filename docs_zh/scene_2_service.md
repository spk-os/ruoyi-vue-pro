# scene_2_service.md

# 场景规则服务 (Scene Rule Service)

## 概述

场景规则服务是 scene_2 模块的核心业务逻辑组件，负责管理和执行物联网(IoT)场景规则。它提供了场景规则的创建、更新、删除、查询以及基于设备消息和定时触发器的规则执行功能。

## 核心功能

### 1. 场景规则生命周期管理

#### createSceneRule
创建新的场景规则：
- 将请求VO转换为DO对象
- 持久化到数据库
- 注册定时触发器（如果规则包含定时触发器）
- 返回新创建规则的ID
- 使用缓存注解清除相关缓存

#### updateSceneRule
更新现有场景规则：
- 验证规则是否存在
- 将请求VO转换为DO对象
- 更新数据库记录
- 更新定时触发器
- 使用缓存

#### updateSceneRuleStatus
更新场景规则状态：
- 验证规则是否存在
- 更新状态字段
- 根据状态管理定时触发器：
  - 启用状态：重新注册定时触发器
  - 禁用状态：暂停定时触发器
- 使用缓存注解清除相关缓存

#### deleteSceneRule
删除场景规则：
- 验证规则是否存在
- 从数据库删除记录
- 注销定时触发器
- 使用缓存注解清除相关缓存

### 2. 场景规则查询

#### getSceneRule
根据ID获取单个场景规则

#### getSceneRulePage
分页查询场景规则列表

#### validateSceneRuleList
批量验证场景规则ID是否存在

#### getSceneRuleListByStatus
根据状态获取场景规则列表

#### getSceneRuleListByProductIdAndDeviceIdFromCache
根据产品ID和设备ID从缓存获取匹配的场景规则列表：
- 查询所有启用状态的规则
- 根据产品ID和设备ID进行过滤匹配
- 使用缓存提高性能
- 忽略租户隔离（因为消息处理时可能没有租户上下文）

### 3. 规则执行

#### executeSceneRuleByDevice
基于设备消息执行场景规则：
- 获取设备和产品信息
- 根据产品ID和设备ID获取匹配的规则列表
- 对匹配的规则执行动作
- 在租户上下文中执行操作

#### executeSceneRuleByTimer
基于定时触发器执行场景规则：
- 获取规则信息
- 验证规则状态和定时触发器存在
- 评估条件组（新增逻辑）
- 如果条件满足，执行规则动作
- 在租户上下文中执行操作

### 4. 条件评估（新增功能）

#### evaluateTimerConditionGroups
评估定时触发器的条件组：
- 如果没有条件组，直接返回true（执行动作）
- 条件组之间是OR关系（任一满足即可）
- 单个条件组内部是AND关系（所有条件都必须满足）

#### evaluateSingleConditionGroup
评估单个条件组：
- 空条件组视为满足
- 检查组内所有条件是否都满足（AND关系）

#### evaluateTimerCondition
评估单个条件（定时触发器专用）：
- 委托给IotTimerConditionEvaluator进行实际评估
- 记录调试日志
- 捕获异常并返回false

### 5. 触发器匹配

#### getMatchedSceneRuleListByMessage
基于设备消息获取匹配的规则场景列表：
- 获取设备和产品信息
- 从缓存获取候选规则列表
- 使用触发器匹配逻辑过滤

#### matchSceneRuleTriggers
匹配场景规则的所有触发器：
- 遍历所有触发器
- 如果任一触发器匹配，则返回true

#### matchSingleTrigger
匹配单个触发器：
- 检查基础触发器匹配
- 检查触发器条件组匹配
- 捕获异常并记录错误

#### isTriggerConditionGroupsMatched
检查触发器的条件分组是否匹配：
- 如果没有条件分组，则认为匹配成功
- 条件分组之间是OR关系
- 条件分组内部是AND关系

#### isTriggerConditionMatched
基于消息判断触发器的子条件是否匹配：
- 委托给IotSceneRuleMatcherManager进行实际匹配
- 捕获异常并记录错误

### 6. 动作执行

#### executeSceneRuleAction
执行规则场景的动作：
- 遍历规则场景列表
- 对每个规则，遍历其动作配置
- 查找对应的动作实现
- 执行动作并记录结果
- 更新最后触发时间

#### updateLastTriggerTime
更新规则场景的最后触发时间：
- 更新数据库中的最后触发时间字段
- 捕获异常并记录错误

## 技术实现

### 依赖
- cn.hutool.core.collection.CollUtil
- cn.hutool.core.collection.ListUtil
- cn.hutool.core.util.ObjUtil
- cn.hutool.extra.spring.SpringUtil
- cn.iocoder.yudao.framework.common.enums.CommonStatusEnum
- cn.iocoder.yudao.framework.common.pojo.PageResult
- cn.iocoder.yudao.framework.common.util.object.BeanUtils
- cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore
- cn.iocoder.yudao.framework.tenant.core.util.TenantUtils
- cn.iocoder.yudao.module.iot.controller.admin.rule.vo.scene.IotSceneRulePageReqVO
- cn.iocoder.yudao.module.iot.controller.admin.rule.vo.scene.IotSceneRuleSaveReqVO
- cn.iocoder.yudao.module.iot.core.mq.message.IotDeviceMessage
- cn.iocoder.yudao.module.iot.dal.dataobject.device.IotDeviceDO
- cn.iocoder.yudao.module.iot.dal.dataobject.product.IotProductDO
- cn.iocoder.yudao.module.iot.dal.dataobject.rule.IotSceneRuleDO
- cn.iocoder.yudao.module.iot.dal.mysql.rule.IotSceneRuleMapper
- cn.iocoder.yudao.module.iot.dal.redis.RedisKeyConstants
- cn.iocoder.yudao.module.iot.enums.rule.IotSceneRuleTriggerTypeEnum
- cn.iocoder.yudao.module.iot.service.device.IotDeviceService
- cn.iocoder.yudao.module.iot.service.product.IotProductService
- cn.iocoder.yudao.module.iot.service.rule.scene.action.IotSceneRuleAction
- cn.iocoder.yudao.module.iot.service.rule.scene.matcher.IotSceneRuleMatcherManager
- cn.iocoder.yudao.module.iot.service.rule.scene.timer.IotSceneRuleTimerHandler
- cn.iocoder.yudao.module.iot.service.rule.scene.timer.IotTimerConditionEvaluator
- jakarta.annotation.Resource
- lombok.extern.slf4j.Slf4j
- org.springframework.cache.annotation.CacheEvict
- org.springframework.cache.annotation.Cacheable
- org.springframework.stereotype.Service
- org.springframework.validation.annotation.Validated
- java.time.LocalDateTime
- java.util.Collection
- java.util.List

### 关键实现细节

1. **缓存策略**：
   - 使用@CacheEvict清除SCENE_RULE_LIST缓存
   - 使用@Cacheable缓存按产品ID和设备ID查询的结果
   - 缓存键格式：#productId + '_' + #deviceId

2. **租户隔离处理**：
   - 使用TenantUtils.execute在指定租户下执行操作
   - 对于getSceneRuleListByProductIdAndDeviceIdFromCache方法，使用@TenantIgnore注解忽略租户隔离
   - 这是因为消息处理时可能没有明确的租户上下文

3. **异常处理**：
   - 使用ServiceExceptionUtil.exception抛出业务异常
   - 记录详细的错误日志便于排查问题
   - 对于非关键操作，捕获异常后返回默认值或继续执行

4. **日志记录**：
   - 使用SLF4J进行日志记录
   - 包含详细的上下文信息（如规则ID、触发器类型等）
   - 不同日志级别：error、warn、info、debug

5. **性能优化**：
   - 使用Hutool工具类进行集合操作
   - 利用缓存减少数据库访问
   - 批量操作减少网络往返
   - 早期返回避免不必要的计算

## 与其他组件的交互

### 输入依赖
- IotSceneRuleMapper: 数据持久化操作
- IotProductService: 产品信息查询
- IotDeviceService: 设备信息查询
- IotSceneRuleMatcherManager: 触发器和条件匹配
- IotSceneRuleAction: 动作执行实现
- IotSceneRuleTimerHandler: 定时触发器管理
- IotTimerConditionEvaluator: 条件评估
- SpringUtil: 获取自身Bean实例（用于内部调用）
- TenantUtils: 租户上下文管理

### 输出影响
- 修改数据库中的IotSceneRuleDO记录
- 影响Redis缓存中的SCENE_RULE_LIST
- 通过IotSceneRuleTimerHandler影响定时任务
- 通过IotSceneRuleAction执行具体的业务动作（如设备控制、告警触发等）

## 使用示例

### 创建场景规则
```java
IotSceneRuleSaveReqVO reqVO = new IotSceneRuleSaveReqVO();
// 设置规则属性...
Long ruleId = sceneRuleService.createSceneRule(reqVO);
```

### 基于设备消息执行规则
```java
IotDeviceMessage message = new IotDeviceMessage();
// 设置消息属性...
sceneRuleService.executeSceneRuleByDevice(message);
```

### 基于定时器执行规则
```java
Long ruleId = 1L;
sceneRuleService.executeSceneRuleByTimer(ruleId);
```

### 查询设备匹配的规则
```java
Long productId = 100L;
Long deviceId = 200L;
List<IotSceneRuleDO> rules = sceneRuleService.getSceneRuleListByProductIdAndDeviceIdFromCache(productId, deviceId);
```

## 注意事项

1. **事务管理**：
   - 方法默认使用Spring的事务管理
   - 需要确保依赖的服务方法也正确处理事务

2. **并发安全**：
   - 依赖数据库的行级锁和乐观锁机制
   - 缓存更新需要注意一致性问题

3. **异常处理**：
   - 业务验证失败会抛出RULE_SCENE_NOT_EXISTS异常
   - 系统异常会被记录但不会中断主要流程（如日志更新失败）

4. **性能考虑**：
   - 缓存命中率对性能影响显著
   - 定时触发器的注册和注销操作应尽量轻量级
   - 条件评估应避免复杂计算

5. **扩展性**：
   - 新增触发器类型只需实现IotSceneRuleAction接口
   - 新增条件类型只需扩展IotSceneRuleConditionOperatorEnum
   - 通过Spring依赖注入自动获取新实现