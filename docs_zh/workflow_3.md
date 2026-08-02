# 工作流模块 (workflow_3) 文档

## 模块概述

工作流模块（`workflow_3`）是AI应用系统的核心模块之一，主要负责管理和执行AI工作流程。该模块通过可视化的方式定义AI任务的执行顺序和逻辑，支持多种AI模型的集成和调用，为AI应用提供灵活的工作流管理能力。

### 主要功能

- **工作流定义**：支持通过可视化界面定义AI工作流的节点和连接关系
- **工作流执行**：提供工作流的执行引擎，支持测试和生产环境的执行
- **工作流管理**：提供工作流的CRUD操作和版本管理
- **AI模型集成**：支持与多种AI模型（如LLM、工具函数等）的集成
- **参数传递**：支持工作流执行过程中的参数传递和结果返回

### 模块架构

```
前端界面 -->|定义工作流| 工作流服务
工作流服务 -->|存储工作流| 数据库
工作流服务 -->|调用AI模型| AI模型服务
工作流服务 -->|执行工作流| 工作流执行引擎
工作流执行引擎 -->|返回结果| 前端界面
工作流执行引擎 -->|调用外部服务| 外部API
```

## 核心组件

### 1. 实体类 (AiWorkflowDO)

工作流的数据库实体类，包含工作流的基本信息和配置。

**主要字段：**
- `id`：工作流ID
- `name`：工作流名称
- `code`：工作流编码（唯一标识）
- `description`：工作流描述
- `graph`：工作流的JSON配置，包含节点和连接信息
- `status`：工作流状态（启用/禁用）
- `createTime`：创建时间
- `updateTime`：更新时间

### 2. 服务接口 (AiWorkflowService)

工作流服务的接口定义，提供工作流的CRUD操作和执行功能。

**主要方法：**
- `createWorkflow(AiWorkflowSaveReqVO createReqVO)`：创建工作流
- `updateWorkflow(AiWorkflowSaveReqVO updateReqVO)`：更新工作流
- `deleteWorkflow(Long id)`：删除工作流
- `getWorkflow(Long id)`：获取工作流详情
- `getWorkflowPage(AiWorkflowPageReqVO pageReqVO)`：分页查询工作流
- `testWorkflow(AiWorkflowTestReqVO testReqVO)`：测试工作流执行

### 3. 服务实现 (AiWorkflowServiceImpl)

工作流服务的具体实现类，包含业务逻辑的实现。

**核心功能：**
- 工作流的CRUD操作
- 工作流代码唯一性校验
- 工作流执行引擎的调用
- AI模型的集成和调用

### 4. 控制器 (AiWorkflowController)

提供RESTful API接口，供前端调用。

**主要接口：**
- `POST /admin-api/ai/workflow`：创建工作流
- `PUT /admin-api/ai/workflow`：更新工作流
- `DELETE /admin-api/ai/workflow/{id}`：删除工作流
- `GET /admin-api/ai/workflow/{id}`：获取工作流详情
- `GET /admin-api/ai/workflow/page`：分页查询工作流
- `POST /admin-api/ai/workflow/test`：测试工作流执行

### 5. 数据访问层 (AiWorkflowMapper)

MyBatis Mapper接口，提供数据库操作。

**主要方法：**
- `selectById(Long id)`：根据ID查询工作流
- `selectByCode(String code)`：根据编码查询工作流
- `selectPage(AiWorkflowPageReqVO pageReqVO)`：分页查询工作流
- `insert(AiWorkflowDO workflow)`：插入工作流
- `updateById(AiWorkflowDO workflow)`：更新工作流
- `deleteById(Long id)`：删除工作流

### 6. 请求/响应VO类

#### AiWorkflowSaveReqVO
工作流保存请求对象，包含工作流的基本信息和配置。

#### AiWorkflowTestReqVO
工作流测试请求对象，包含工作流ID和测试参数。

#### AiWorkflowPageReqVO
工作流分页查询请求对象，包含分页参数和过滤条件。

#### AiWorkflowRespVO
工作流响应对象，包含工作流的详细信息。

### 7. 错误码 (ErrorCodeConstants)

定义工作流模块的错误码，包括：
- `WORKFLOW_NOT_EXISTS`：工作流不存在
- `WORKFLOW_CODE_EXISTS`：工作流编码已存在

### 8. AI模型服务 (AiModelService)

提供AI模型的相关操作，包括：
- 获取LLM模型提供者
- 模型参数配置
- 模型调用

## 工作流执行引擎

工作流模块使用TinyFlow作为工作流执行引擎，支持以下特性：

### 1. 节点类型

- **LLM节点**：调用大语言模型
- **内部节点**：执行内部逻辑
- **工具节点**：调用外部工具或API

### 2. 参数传递

工作流执行过程中支持参数的传递和结果的返回，支持动态参数和静态参数。

### 3. 错误处理

工作流执行过程中支持错误处理和重试机制。

## API接口文档

### 创建工作流

**接口地址：** `POST /admin-api/ai/workflow`

**请求参数：**
```json
{
  "name": "工作流名称",
  "code": "WORKFLOW_001",
  "description": "工作流描述",
  "graph": "工作流JSON配置",
  "status": 1
}
```

**响应参数：**
```json
{
  "code": 0,
  "message": "success",
  "data": 1
}
```

### 更新工作流

**接口地址：** `PUT /admin-api/ai/workflow`

**请求参数：**
```json
{
  "id": 1,
  "name": "更新后的工作流名称",
  "code": "WORKFLOW_001",
  "description": "更新后的工作流描述",
  "graph": "更新后的工作流JSON配置",
  "status": 1
}
```

**响应参数：**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 删除工作流

**接口地址：** `DELETE /admin-api/ai/workflow/{id}`

**请求参数：**
```
id: 1
```

**响应参数：**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 获取工作流详情

**接口地址：** `GET /admin-api/ai/workflow/{id}`

**请求参数：**
```
id: 1
```

**响应参数：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "name": "工作流名称",
    "code": "WORKFLOW_001",
    "description": "工作流描述",
    "graph": "工作流JSON配置",
    "status": 1,
    "createTime": "2023-01-01 00:00:00",
    "updateTime": "2023-01-01 00:00:00"
  }
}
```

### 分页查询工作流

**接口地址：** `GET /admin-api/ai/workflow/page`

**请求参数：**
```
pageNo: 1
pageSize: 10
name: 工作流名称（可选）
code: 工作流编码（可选）
status: 工作流状态（可选）
```

**响应参数：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "工作流名称",
        "code": "WORKFLOW_001",
        "description": "工作流描述",
        "status": 1,
        "createTime": "2023-01-01 00:00:00",
        "updateTime": "2023-01-01 00:00:00"
      }
    ],
    "total": 1
  }
}
```

### 测试工作流执行

**接口地址：** `POST /admin-api/ai/workflow/test`

**请求参数：**
```json
{
  "id": 1,
  "params": {
    "param1": "value1",
    "param2": "value2"
  }
}
```

**响应参数：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "result": "工作流执行结果"
  }
}
```

## 使用示例

### 1. 创建工作流

```java
AiWorkflowSaveReqVO createReqVO = new AiWorkflowSaveReqVO();
createReqVO.setName("测试工作流");
createReqVO.setCode("TEST_WORKFLOW");
createReqVO.setDescription("这是一个测试工作流");
createReqVO.setGraph("{\"nodes\":[{\"type\":\"llmNode\",\"data\":{\"llmId\":1}}]}");
createReqVO.setStatus(1);

Long workflowId = aiWorkflowService.createWorkflow(createReqVO);
```

### 2. 测试工作流执行

```java
AiWorkflowTestReqVO testReqVO = new AiWorkflowTestReqVO();
testReqVO.setId(1L);
testReqVO.setParams(new HashMap<>());
testReqVO.getParams().put("input", "你好");

Object result = aiWorkflowService.testWorkflow(testReqVO);
```

### 3. 分页查询工作流

```java
AiWorkflowPageReqVO pageReqVO = new AiWorkflowPageReqVO();
pageReqVO.setPageNo(1);
pageReqVO.setPageSize(10);
pageReqVO.setName("测试");

PageResult<AiWorkflowDO> pageResult = aiWorkflowService.getWorkflowPage(pageReqVO);
```

## 与其他模块的集成

### 1. AI模型服务集成

工作流模块通过`AiModelService`与AI模型服务集成，支持调用多种AI模型。

### 2. 前端界面集成

前端界面通过RESTful API与工作流服务交互，实现工作流的可视化定义和执行。

### 3. 数据库集成

工作流模块使用MyBatis与数据库交互，持久化工作流配置。

## 最佳实践

### 1. 工作流设计

- **模块化设计**：将复杂的工作流拆分为多个简单的子工作流
- **错误处理**：在工作流中添加错误处理节点，确保工作流的健壮性
- **参数管理**：合理设计工作流的输入和输出参数，便于维护和调试

### 2. 性能优化

- **缓存**：对频繁访问的工作流配置进行缓存
- **异步执行**：对于耗时较长的工作流，考虑使用异步执行
- **批量处理**：对于批量任务，考虑使用批量处理的工作流

### 3. 安全性

- **权限控制**：确保工作流的创建、修改和执行权限受到严格控制
- **数据验证**：对工作流的输入参数进行严格的验证，防止注入攻击
- **日志记录**：记录工作流的执行日志，便于审计和问题排查

## 常见问题

### 1. 工作流执行失败

**可能原因：**
- 工作流配置错误
- AI模型服务不可用
- 参数传递错误

**解决方案：**
- 检查工作流配置
- 检查AI模型服务状态
- 检查输入参数

### 2. 工作流编码冲突

**可能原因：**
- 工作流编码重复

**解决方案：**
- 修改工作流编码，确保唯一性

### 3. 工作流执行超时

**可能原因：**
- 工作流执行时间过长
- 网络延迟

**解决方案：**
- 优化工作流设计，减少执行时间
- 检查网络连接

## 总结

工作流模块（`workflow_3`）为AI应用系统提供了灵活的工作流管理能力，支持工作流的定义、执行和管理。通过与AI模型服务的集成，可以实现复杂的AI任务处理流程。模块设计遵循了模块化、可扩展和易维护的原则，为AI应用的开发和运维提供了有力的支持。
