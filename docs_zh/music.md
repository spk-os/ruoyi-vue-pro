# 音乐模块文档

## 模块概述

音乐模块（AiMusic）是 Yudao AI 模块的一部分，提供 AI 音乐生成和管理功能。用户可以通过该模块创建、查询、更新 AI 生成的音乐记录，并集成了 Suno AI 音乐生成服务，支持一键生成音乐并同步生成状态。

## 架构设计

### 组件结构

音乐模块主要包含以下组件：

- **控制器层**（Controller）：`AiMusicController` - 处理 HTTP 请求，提供 RESTful API。
- **服务层**（Service）：`AiMusicServiceImpl` - 实现业务逻辑，包括音乐记录的 CRUD 操作和与 Suno AI 的交互。
- **数据访问层**（DAO）：`AiMusicDO` - 音乐实体的数据对象，映射到数据库表。
- **任务层**（Job）：`AiSunoSyncJob` - 定时任务，用于同步 Suno 音乐生成任务的状态。
- **视图对象层**（VO）：包含各种请求和响应对象，用于前后端数据传输。

### 依赖关系

```
图 1：组件依赖关系
graph TD
    A[AiMusicController] --> B[AiMusicService]
    B --> C[AiMusicDAO]
    B --> D[Suno AI 客户端]
    B --> E[AiSunoSyncJob]
    F[其他服务] --> B
    G[配置中心] --> B
```

### 数据流

#### 音乐创建流程
```
图 2：音乐创建流程
sequenceDiagram
    participant User as 用户
    participant Controller as AiMusicController
    participant Service as AiMusicService
    participant DAO as AiMusicDAO
    participant Suno as Suno AI

    User->>Controller: 发送音乐生成请求 (AiSunoGenerateReqVO)
    Controller->>Service: 调用生成音乐服务
    Service->>Suno: 调用 Suno API 生成音乐
    Suno-->>Service: 返回生成任务ID
    Service->>DAO: 保存音乐记录（状态为处理中）
    DAO-->>Service: 保存成功
    Service-->>Controller: 返回音乐记录ID和任务ID
    Controller-->>User: 返回响应

    Note over Service,Dao: 后台通过 AiSunoSyncJob 定时轮询 Suno 任务状态
    Suno->>Service: 回调或轮询获取结果
    Service->>DAO: 更新音乐记录状态和结果
    DAO-->>Service: 更新成功
```

#### 音乐查询流程
```
图 3：音乐查询流程
sequenceDiagram
    participant User as 用户
    participant Controller as AiMusicController
    participant Service as AiMusicService
    participant DAO as AiMusicDAO

    User->>Controller: 发送分页查询请求 (AiMusicPageReqVO)
    Controller->>Service: 调用分页查询服务
    Service->>DAO: 查询音乐列表和总数
    DAO-->>Service: 返回查询结果
    Service-->>Controller: 返回分页响应 (PageResult<AiMusicRespVO>)
    Controller-->>User: 返回查询结果
```

## 接口说明

### 控制器接口

| 方法 | 路径 | 说明 | 请求类型 | 备注 |
|------|------|------|----------|------|
| 分页查询音乐 | `/music/page` | 获取音乐列表（支持分页和条件过滤） | GET | 需要登录 |
| 获取音乐详情 | `/music/get/{id}` | 根据 ID 获取音乐详情 | GET | 需要登录 |
| 创建音乐生成任务 | `/music/generate` | 提交音乐生成请求到 Suno AI | POST | 需要登录 |
| 更新音乐信息 | `/music/update` | 更新音乐信息（仅限创建者或管理员） | PUT | 需要登录 |
| 更新我的音乐 | `/music/update-my` | 更新当前用户的音乐信息 | PUT | 需要登录 |
| 删除音乐 | `/music/delete/{id}` | 删除音乐记录（软删） | DELETE | 需要登录或管理员 |

### 服务接口

`AiMusicService` 提供以下核心方法：

- `PageResult<AiMusicRespVO> page(AiMusicPageReqVO reqVO)`：分页查询音乐
- `AiMusicRespVO get(Long id)`：根据 ID 获取音乐详情
- `Long generate(AiSunoGenerateReqVO reqVO)`：创建音乐生成任务并返回音乐 ID
- `void update(AiMusicUpdateReqVO reqVO)`：更新音乐信息
- `void updateMy(AiMusicUpdateMyReqVO reqVO)`：更新当前用户的音乐
- `void delete(Long id)`：删除音乐记录

### 数据模型

#### AiMusicRespVO
音乐响应对象，包含以下字段：
- `id`: 音乐 ID
- `title`: 音乐标题
- `description`: 音乐描述
- `coverUrl`: 封面图 URL
- `audioUrl`: 音频文件 URL
- `status`: 生成状态（如：处理中、成功、失败）
- `sunoTaskId`: Suno 任务 ID
- `createTime`: 创建时间
- `updateTime`: 更新时间

#### AiSunoGenerateReqVO
Suno 音乐生成请求对象，包含：
- `prompt`: 音乐生成提示词
- `style`: 音乐风格（可选）
- `title`: 音乐标题（可选）
- `makeInstrumental`: 是否纯音乐（可选）

## 配置说明

音乐模块依赖以下配置（通常在 `application.yml` 或 `application-{profile}.yml` 中配置）：

```yaml
# Suno AI 配置
suno:
  api-key: your_suno_api_key  # Suno API 密钥
  api-base-url: https://api.suno.ai/api/v1  # Suno API 基础地址
  callback-url: https://your-domain.com/api/ai/music/suno/callback  # Suno 回调地址（如果支持回调）

# 音乐文件存储配置
music:
  upload-dir: /var/uploads/music  # 音乐文件存储目录
  url-prefix: /music  # 音频文件访问前缀
```

## 使用示例

### 1. 生成音乐
```bash
curl -X POST 'http://localhost:8080/api/ai/music/generate' \
  -H 'Authorization: Bearer <your_token>' \
  -H 'Content-Type: application/json' \
  -d '{
        "prompt": "一个轻快的电子乐曲，适合夏日驾驶",
        "style": "electronic",
        "title": "夏日驾驶",
        "makeInstrumental": true
      }'
```

### 2. 查询音乐列表
```bash
curl -X GET 'http://localhost:8080/api/ai/music/page?page=1&size=10' \
  -H 'Authorization: Bearer <your_token>'
```

### 3. 更新音乐信息
```bash
curl -X PUT 'http://localhost:8080/api/ai/music/update' \
  -H 'Authorization: Bearer <your_token>' \
  -H 'Content-Type: application/json' \
  -d '{
        "id": 1,
        "title": "更新后的标题",
        "description": "更新后的描述"
      }'
```

## 注意事项

1. 音乐生成是异步过程，提交后会返回音乐 ID 和 Suno 任务 ID，实际音频文件生成需要一定时间。
2. 系统通过定时任务（`AiSunoSyncJob`）轮询 Suno 任务状态，更新音乐记录。
3. 音频文件和封面图会存储在配置指定的目录中，通过静态资源访问。
4. 只有音乐的创建者或管理员可以修改或删除音乐记录。
5. 建议在生产环境中配置 Suno API 的回调 URL，以实时获取生成结果，减少轮询频率。

## 与其他模块的关系

音乐模块主要是独立的功能模块，但可能与以下模块有交互：

- **存储模块**：如果使用统一的文件存储服务（如 MinIO、OSS），则音乐文件和封面图会上传到统一存储。
- **消息通知模块**：音乐生成完成后，可通过消息通知模块发送提醒给用户。
- **工作流模块**：音乐生成流程可集成到工作流中，作为某个业务流程的一个步骤。