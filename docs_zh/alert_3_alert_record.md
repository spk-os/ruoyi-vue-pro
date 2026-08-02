# alert_3_alert_record.md

# 告警记录管理子模块文档

## 子模块概述

告警记录管理子模块是alert_3模块的重要组成部分，负责记录、查询和处理系统产生的告警事件。当监控到满足告警条件的事件时，系统会生成告警记录并存储在数据库中，用户可以通过此子模块查看告警详情、处理告警以及进行告警统计分析。

## 核心组件

告警记录管理子模块包含以下核心组件：

1. **控制器 (Controller)**: IotAlertRecordController
2. **服务接口和实现 (Service)**: IotAlertRecordService 和 IotAlertRecordServiceImpl
3. **数据访问接口 (Mapper)**: IotAlertRecordMapper
4. **数据对象 (DO)**: IotAlertRecordDO
5. **值对象 (VO)**: 
   - IotAlertRecordPageReqVO (分页查询请求)
   - IotAlertRecordProcessReqVO (处理请求)
   - IotAlertRecordRespVO (响应)

## 详细设计

### 控制器层 (IotAlertRecordController)

控制器负责处理HTTP请求，将请求路由到相应的服务方法。主要提供以下RESTful API接口：

- GET /iot/alert-record/get：获取单个告警记录详情
- GET /iot/alert-record/page：分页查询告警记录列表
- PUT /iot/alert-record/process：处理告警记录（批量处理）

### 服务层 (IotAlertRecordServiceImpl)

服务层实现业务逻辑，是告警记录管理的核心。主要职责包括：

1. **查询操作**：根据各种条件查询告警记录
2. **处理操作**：批量处理告警记录，标记为已处理并记录处理备注
3. **数据转换**：在VO和DO之间进行转换
4. **数据持久化**：通过Mapper接口与数据库交互

关键方法：
- `getAlertRecord(Long id)`: 获取告警记录详情
- `getAlertRecordPage(IotAlertRecordPageReqVO pageReqVO)`: 分页查询告警记录
- `getAlertRecordListBySceneRuleId(Long sceneRuleId, Long deviceId, Boolean processStatus)`: 根据场景规则ID、设备ID和处理状态查询告警记录
- `processAlertRecordList(Collection<Long> ids, String processRemark)`: 批量处理告警记录

### 数据访问层 (IotAlertRecordMapper)

使用MyBatis框架，定义与iot_alert_record表的交互操作：
- 按ID查询记录
- 分页查询记录
- 根据场景规则ID、设备ID和处理状态查询记录列表
- 批量更新记录的处理状态和备注
- 插入新记录（由服务层内部调用的createAlertRecord方法对应）

### 数据对象 (IotAlertRecordDO)

对应数据库表iot_alert_record的实体类，包含所有字段及其注解：
- @TableName: 指定表名
- @TableId: 指定主键
- @TableField: 指定字段属性和类型处理器（特别是复杂类型IotDeviceMessage的Jackson3TypeHandler）
- 各种注释说明字段含义和关联关系

### 值对象 (VO)

#### IotAlertRecordPageReqVO
用于分页查询的请求对象，继承通用分页参数，包含：
- 配置ID（可选，精确匹配）
- 配置名称（可选，模糊查询）
- 场景规则ID（可选，精确匹配）
- 设备ID（可选，精确匹配）
- 产品ID（可选，精确匹配）
- 处理状态（可选，精确匹配）
- 创建时间范围（可选）

#### IotAlertRecordProcessReqVO
用于处理告警记录的请求对象，包含：
- 告警记录ID（必填）
- 处理结果/备注（必填）

#### IotAlertRecordRespVO
用于响应的告警记录对象，包含DO的所有字段以及可能的扩展字段。

## 业务流程

### 告警记录生成流程
（注意：告警记录的生成通常发生在业务触发点，而不是通过此子模块的直接API）

1. 当设备上报数据满足某个场景规则的条件时
2. 业务服务调用IotAlertRecordService的createAlertRecord方法
3. 方法接收告警配置、场景规则ID、设备消息和设备信息作为参数
4. 构建IotAlertRecordDO对象，将相关信息填入对应字段
5. 调用Mapper插入数据库
6. 返回新创建记录的ID

### 查询告警记录流程
1. 接收前端请求，参数封装为IotAlertRecordPageReqVO或直接的ID
2. 参数基本校验（如ID非空等）
3. 调用Mapper查询数据库
4. 将DO转换为VO（如果需要）
5. 返回查询结果

### 处理告警记录流程
1. 接收前端请求，参数封装为IotAlertRecordProcessReqVO
2. 参数校验（ID非空、备注非空等）
3. 将单个请求包装为集合（为了复用批量处理方法）
4. 调用service的processAlertRecordList方法
5. 在service层，如果ID集合不为空，则构建更新对象
6. 更新对象设置处理状态为true和处理备注
7. 调用Mapper批量更新记录
8. 返回成功标志

## 与其他组件的交互

### 与告警配置的关联
- 告警记录通过configId和configName字段与告警配置关联
- 这些字段是冗余字段，在创建告警记录时从告警复制过来
- 这种设计避免了查询时的关联查询，提高了查询性能

### 与场景规则的关联
- 告警记录通过sceneRuleId字段与场景规则关联
- 此字段用于查询时过滤和关联，但不存储冗余的规则名称等信息
- 在查询时可以通过此ID关联到场景规则表获取更多信息

### 与设备和产品的关联
- 告警记录通过productId和deviceId字段分别与产品和设备关联
- 这些字段用于查询时过滤和关联
- 在创建告警记录时，如果提供了设备信息，则会填充这些字段

### 与设备消息的关联
- 告警记录通过deviceMessage字段保存触发告警的设备原始消息
- 此字段使用Jackson3TypeHandler进行JSON序列化/反序列化
- 保存完整的设备消息有助于问题排查和审计

## 数据模型详解

### IotAlertRecordDO (告警记录数据对象)

| 字段名 | 类型 | 说明 | 关联 |
|--------|------|------|------|
| id | Long | 记录编号，主键 | - |
| configId | Long | 告警配置编号（冗余） | IotAlertConfigDO.id |
| configName | String | 告警配置名称（冗余） | IotAlertConfigDO.name |
| configLevel | Integer | 告警级别（冗余） | IotAlertConfigDO.level |
| sceneRuleId | Long | 场景规则编号 | IotSceneRuleDO.id |
| productId | Long | 产品编号 | IotProductDO.id |
| deviceId | Long | 设备编号 | IotDeviceDO.id |
| deviceMessage | IotDeviceMessage | 触发的设备消息 | - |
| processStatus | Boolean | 是否处理 | - |
| processRemark | String | 处理结果（备注） | - |

### 重要字段说明

1. **冗余字段设计**：
   - configId、configName、configLevel是冗余字段，直接从告警配置复制而来
   - 这种设计避免了查询告警记录时需要关联告警配置表的JOIN操作
   - 提高了查询性能，特别是在需要展示告警列表时

2. **设备消息存储**：
   - deviceMessage字段存储触发告警的原始设备消息
   - 使用自定义类型处理器（Jackson3TypeHandler）进行JSON序列化
   - 保存完整的原始数据有助于故障排查和审计追踪

3. **处理状态**：
   - processStatus字段用于标记告警是否已被处理
   - 初始状态为false（未处理），处理后设置为true
   - 通过此字段可以实现未处理告警的快速查询

4. **处理备注**：
   - processRemark字段记录处理人的处理结果或备注
   - 为问题跟踪和处理过程审计提供了重要信息

## 与其他模块的交互

### 与告警配置模块的交互
- 虽然告警记录模块不直接依赖告警配置服务
- 但在创建告警记录时需要告警配置对象作为参数
- 这种设计避免了循环依赖，同时保证了数据的一致性

### 与设备模块的交互
- 告警记录需要设备信息（产品ID和设备ID）来定位问题设备
- 这些信息通常由触发告警的业务服务提供
- 在查询时，可以通过这些ID关联到设备表获取更详细的设备信息

### 与场景规则模块的交互
- 告警记录通过sceneRuleId与触发告警的场景规则关联
- 这使得可以统计哪些规则触发了最多的告警
- 有助于优化监控策略和调整阈值

## 异常处理

告警记录管理子模块相对简单，主要的异常情况包括：

1. **参数验证异常**：
   - 处理告警时，如果ID为空或备注为空，会在控制器层通过参数验证拦截
   - 返回参数错误的标准响应

2. **数据不存在异常**：
   - 获取告警记录详情时，如果ID不存在，返回空结果而不是抛出异常
   - 这是基于业务考虑，避免因为查询不存在的记录而中断流程

3. **数据库访问异常**：
   - 由Spring的事务管理和MyBatis框架处理
   - 通常会回滚事务并转换为运行时异常
   - 在全局异常处理器中统一处理为500错误

由于告警记录主要是查询和更新操作，业务异常较少，主要依赖于参数验证和标准的数据库异常处理机制。

## 性能考虑

1. **索引优化**：
   - 建议在iot_alert_record表的以下字段上建立索引：
     - configId（告警配置ID）
     - sceneRuleId（场景规则ID）
     - deviceId（设备ID）
     - processStatus（处理状态）
     - create_time（创建时间）
   - 复合索引：(processStatus, create_time) 用于快速查询未处理的最近告警
   - 复合索引：(configId, processStatus) 用于快速查询特定配置的未处理告警

2. **分页查询优化**：
   - 使用MyBatis的RowBounds或数据库特有的分页语句（如MySQL的LIMIT）
   - 避免一次性加载大量告警记录导致内存溢出

3. **批量操作**：
   - 处理告警支持批量操作，减少数据库交互次数
   - 特别适用于批量确认或批量忽略告警的场景

4. **字段冗余**：
   - 通过冗余字段减少JOIN操作，提高查询性能
   - 特别是在告警列表展示场景中，避免了多表关联

5. **历史数据归档**：
   - 对于历史告警数据，可以考虑定期归档到历史表
   - 保持活跃表的大小在可控范围内，维持查询性能

## 安全考虑

1. **权限控制**：
   - 所有接口都需要相应的权限才能访问
   - 查询告警记录需要"iot:alert-record:query"权限
   - 处理告警记录需要"iot:alert-record:process"权限

2. **数据隔离**：
   - 在多租户环境中，需要确保用户只能访问自己租户的告警记录
   - 这通常通过在查询条件中添加租户ID过滤来实现
   - 虽然当前实现中未显式体现，但可以通过BaseDO或自定义拦截器实现

3. **输入验证**：
   - 所有输入参数都经过非空验证
   - ID参数通常需要是正数
   - 处理备注有长度限制以防止过长文本导致的问题

4. **防止SQL注入**：
   - 使用MyBatis预编译语句或参数绑定
   - 避免拼接SQL字符串

## 使用示例

### 查询告警记录列表请求
```json
{
  "pageNum": 1,
  "pageSize": 10,
  "configId": 10001,
  "processStatus": false,
  "createTimeBegin": "2023-01-01 00:00:00",
  "createTimeEnd": "2023-12-31 23:59:59"
}
```

### 响应示例
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 20001,
        "configId": 10001,
        "configName": "温度过高告警",
        "configLevel": 2,
        "sceneRuleId": 5001,
        "productId": 3001,
        "deviceId": 4001,
        "deviceMessage": {"temperature": 85.5, "timestamp": "2023-06-15T10:30:00Z"},
        "processStatus": false,
        "processRemark": null,
        "createTime": "2023-06-15 10:30:05"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "totalPage": 1
  }
}
```

### 处理告警记录请求
```json
{
  "id": 20001,
  "processRemark": "已确认，维修人员正在处理"
}
```

### 响应示例
```json
{
  "code": 200,
  "message": "成功",
  "data": true
}
```