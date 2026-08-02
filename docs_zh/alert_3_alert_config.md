# alert_3_alert_config.md

# 告警配置管理子模块文档

## 子模块概述

告警配置管理子模块是alert_3模块的核心组成部分，负责IoT告警规则的创建、修改、删除和查询。通过该子模块，用户可以定义何时触发告警、告警的严重级别、告警的接收方式以及告警的接收人员等信息。

## 核心组件

告警配置管理子模块包含以下核心组件：

1. **控制器 (Controller)**: IotAlertConfigController
2. **服务接口和实现 (Service)**: IotAlertConfigService 和 IotAlertConfigServiceImpl
3. **数据访问接口 (Mapper)**: IotAlertConfigMapper
4. **数据对象 (DO)**: IotAlertConfigDO
5. **值对象 (VO)**: 
   - IotAlertConfigPageReqVO (分页查询请求)
   - IotAlertConfigSaveReqVO (创建/更新请求)
   - IotAlertConfigRespVO (响应)

## 详细设计

### 控制器层 (IotAlertConfigController)

控制器负责处理HTTP请求，将请求路由到相应的服务方法。主要提供以下RESTful API接口：

- POST /iot/alert-config/create：创建告警配置
- PUT /iot/alert-config/update：更新告警配置
- DELETE /iot/alert-config/delete：删除告警配置
- GET /iot/alert-config/get：获取单个告警配置详情
- GET /iot/alert-config/page：分页查询告警配置列表
- GET /iot/alert-config/simple-list：获取启用状态的告警配置简单列表

### 服务层 (IotAlertConfigServiceImpl)

服务层实现业务逻辑，是告警配置管理的核心。主要职责包括：

1. **参数校验**：验证请求参数的合法性
2. **业务规则验证**：确保关联的场景规则和用户存在
3. **数据转换**：在VO和DO之间进行转换
4. **数据持久化**：通过Mapper接口与数据库交互
5. **异常处理**: 抛出业务异常以供全局异常处理器处理

关键方法：
- `createAlertConfig(IotAlertConfigSaveReqVO createReqVO)`: 创建告警配置
- `updateAlertConfig(IotAlertConfigSaveReqVO updateReqVO)`: 更新告警配置
- `deleteAlertConfig(Long id)`: 删除告警配置
- `getAlertConfig(Long id)`: 获取告警配置详情
- `getAlertConfigPage(IotAlertConfigPageReqVO pageReqVO)`: 分页查询告警配置
- `getAlertConfigListByStatus(Integer status)`: 根据状态获取告警配置列表
- `getAlertConfigListBySceneRuleIdAndStatus(Long sceneRuleId, Integer status)`: 根据场景规则ID和状态获取告警配置列表

### 数据访问层 (IotAlertConfigMapper)

使用MyBatis框架，定义与iot_alert_config表的交互操作：
- 插入记录
- 按ID更新记录
- 按ID删除记录
- 按ID查询记录
- 分页查询记录
- 按状态查询记录列表
- 按场景规则ID和状态查询记录列表

### 数据对象 (IotAlertConfigDO)

对应数据库表iot_alert_config的实体类，包含所有字段及其注解：
- @TableName: 指定表名
- @TableId: 指定主键
- @TableField: 指定字段属性和类型处理器
- 各种注释说明字段含义和关联关系

### 值对象 (VO)

#### IotAlertConfigPageReqVO
用于分页查询的请求对象，继承通用分页参数，包含：
- 配置名称（模糊查询）
- 配置状态
- 配置级别
- 创建时间范围

#### IotAlertConfigSaveReqVO
用于创建和更新告警配置的请求对象，包含：
- 配置名称（必填）
- 配置描述
- 配置级别（必填，关联字典）
- 配置状态（必填，关联CommonStatusEnum）
- 关联的场景规则ID列表（必填）
- 接收用户ID列表（必填）
- 接收类型列表（必填，关联IotAlertReceiveTypeEnum）
- 短信模板编号（当接收类型包含短信时必填）
- 邮件模板编号（当接收类型包含邮件时必填）
- 站内信模板编号（当接收类型包含站内信时必填）

## 业务流程

### 创建告警配置流程
1. 接收前端请求，参数封装为IotAlertConfigSaveReqVO
2. 参数校验（非空、格式等）
3. 验证关联的场景规则ID是否存在（调用IotSceneRuleService）
4. 验证关联的用户ID是否存在（调用AdminUserApi）
5. 验证接收类型对应的模板是否已配置
6. 将VO转换为DO对象
7. 调用Mapper插入数据库
8. 返回新创建记录的ID

### 更新告警配置流程
1. 接收前端请求，参数封装为IotAlertConfigSaveReqVO
2. 参数校验（非空、格式等）
3. 验证告警配置ID存在
4. 验证关联的场景规则ID是否存在（调用IotSceneRuleService）
5. 验证关联的用户ID是否存在（调用AdminUserApi）
6. 验证接收类型对应的模板是否已配置
7. 将VO转换为DO对象
8. 调用Mapper更新数据库
9. 返回成功标志

### 查询告警配置流程
1. 接收查询参数（ID或分页条件）
2. 参数基本校验
3. 调用Mapper查询数据库
4. 将DO转换为VO（如果需要）
5. 返回查询结果

## 与其他组件的交互

### 与场景规则服务的交互
- 在创建和更新告警配置时，需要验证所关联的场景规则是否存在
- 通过IotSceneRuleService的validateSceneRuleList方法实现
- 这是一个延迟加载的依赖（@Lazy），用于避免潜在的循环依赖

### 与用户服务的交互
- 在创建和更新告警配置时，需要验证所关联的用户是否存在
- 通过AdminUserApi的validateUserList方法实现
- 在查询接口中，还需要批量获取用户信息以填充返回数据

### 与数据访问层的交互
- 通过IotAlertConfigMapper接口执行所有数据库操作
- 使用MyBatis的XML映射或注解方式定义SQL语句
- 支持复杂查询条件和分页功能

## 异常处理

子模块定义了以下业务异常：
- ALERT_CONFIG_NOT_EXISTS: 当尝试更新或删除不存在的告警配置时抛出
- ALERT_CONFIG_MAIL_TEMPLATE_REQUIRED: 当接收类型包含邮件但未配置邮件模板时抛出
- ALERT_CONFIG_SMS_TEMPLATE_REQUIRED: 当接收类型包含短信但未配置短信模板时抛出
- ALERT_CONFIG_NOTIFY_TEMPLATE_REQUIRED: 当接收类型包含站内信但未配置站内信模板时抛出

所有异常通过ServiceExceptionUtil.exception()方法抛出，由全局异常处理器统一捕获并转换为标准错误响应。

## 性能考虑

1. **索引优化**: 建议在iot_alert_config表的status、level和create_time字段上建立索引
2. **批量验证**: 使用工具类的批量验证方法减少服务调用次数
3. **延迟加载**: 对于不常用的关联服务使用@Lazy延迟加载
4. **结果集映射**: 使用MyBatis的结果集映射减少Java对象创建开销
5. **分页查询**: 所有列表接口都支持分页，防止一次性加载过多数据

## 安全考虑

1. **权限控制**: 所有接口都需要相应的权限才能访问
2. **输入验证**: 所有输入参数都经过非空、格式和业务规则验证
3. **SQL注入防护**: 使用MyBatis预编译语句防止SQL注入
4. **数据权限**: 在多租户场景下，需要确保用户只能访问自己租户的数据（虽然当前实现中未显式体现，但通过BaseDO可能实现）

## 配置说明

告警配置支持三种通知方式：
1. **短信 (SMS)**: 需要配置短信模板编号（smsTemplateCode）
2. **邮件 (MAIL)**: 需要配置邮件模板编号（mailTemplateCode）
3. **站内信 (NOTIFY)**: 需要配置站内信模板编号（notifyTemplateCode）

用户可以选择一种或多种通知方式组合使用。当选择某种通知方式时，对应的模板配置成为必填项。

## 使用示例

### 创建告警配置请求
```json
{
  "name": "温度过高告警",
  "description": "当设备温度超过80度时触发告警",
  "level": 2,
  "status": 1,
  "sceneRuleIds": [1001, 1002],
  "receiveUserIds": [1, 2, 3],
  "receiveTypes": [1, 2], // 1:短信, 2:邮件
  "smsTemplateCode": "TEMP_HIGH_SMS",
  "mailTemplateCode": "TEMP_HIGH_MAIL"
}
```

### 响应示例
```json
{
  "code": 200,
  "message": "成功",
  "data": 10001
}
```