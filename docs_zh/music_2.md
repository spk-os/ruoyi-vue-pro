# AI音乐模块 (music_2)

## 模块概述

AI音乐模块是系统中负责AI音乐生成功能的核心模块，主要提供基于AI技术的音乐创作服务。用户可以通过描述词或歌词来生成音乐，系统会调用第三方AI音乐平台（如Suno）完成音乐生成，并将生成结果保存到数据库中。

该模块主要包含以下功能：
- 音乐生成：支持描述词模式和歌词模式生成音乐
- 音乐管理：用户可以查看、编辑、删除自己的音乐作品
- 音乐同步：定期同步音乐生成状态，更新进度信息
- 音乐分享：支持将生成的音乐设置为公开或私有

## 架构设计

### 模块结构

```
yudao-module-ai/
└── src/main/java/cn/iocoder/yudao/module/ai/
    ├── controller/admin/music/          # 控制层
    │   ├── AiMusicController.java        # 音乐控制器
    │   └── vo/                          # 视图对象
    │       ├── AiMusicRespVO.java        # 音乐响应VO
    │       ├── AiMusicPageReqVO.java     # 音乐分页请求VO
    │       ├── AiMusicUpdateReqVO.java   # 音乐更新请求VO
    │       ├── AiMusicUpdateMyReqVO.java # 我的音乐更新请求VO
    │       └── AiSunoGenerateReqVO.java  # Suno音乐生成请求VO
    ├── service/music/                   # 服务层
    │   ├── AiMusicService.java           # 音乐服务接口
    │   └── AiMusicServiceImpl.java      # 音乐服务实现
    ├── dal/dataobject/music/            # 数据访问层
    │   └── AiMusicDO.java               # 音乐数据对象
    ├── enums/music/                     # 枚举类
    │   ├── AiMusicStatusEnum.java       # 音乐状态枚举
    │   └── AiMusicGenerateModeEnum.java # 音乐生成模式枚举
    └── job/music/                       # 定时任务
        └── AiSunoSyncJob.java           # Suno同步任务
```

### 核心类说明

#### 1. AiMusicDO (数据对象)

```java
@TableName(value = "ai_music", autoResultMap = true)
@Data
public class AiMusicDO extends BaseDO {
    // 编号、用户编号、音乐名称、歌词、图片地址、音频地址、视频地址、音乐状态、生成模式、描述词、平台、模型、音乐风格标签、音乐时长、是否公开、任务编号、错误信息、创建时间
}
```

#### 2. AiMusicService (服务接口)

```java
public interface AiMusicService {
    List<Long> generateMusic(Long userId, AiSunoGenerateReqVO reqVO);
    Integer syncMusic();
    void updateMusic(@Valid AiMusicUpdateReqVO updateReqVO);
    void updateMyMusic(@Valid AiMusicUpdateMyReqVO updateReqVO, Long userId);
    void deleteMusic(Long id);
    void deleteMusicMy(Long id, Long userId);
    AiMusicDO getMusic(Long id);
    PageResult<AiMusicDO> getMusicPage(AiMusicPageReqVO pageReqVO);
    PageResult<AiMusicDO> getMusicMyPage(AiMusicPageReqVO pageReqVO, Long userId);
}
```

#### 3. AiMusicServiceImpl (服务实现)

主要实现了音乐生成、同步、更新、删除等核心功能。

#### 4. AiMusicController (控制器)

提供RESTful API接口，包括音乐生成、查询、更新、删除等操作。

#### 5. AiSunoSyncJob (定时任务)

定期同步Suno平台的音乐生成状态，更新本地数据库中的音乐状态信息。

## 数据流设计

### 音乐生成流程

```
用户请求 → AiMusicController → AiMusicServiceImpl → 调用SunoApi生成音乐 → 构建AiMusicDO对象 → 保存到数据库 → 返回音乐ID
```

### 音乐同步流程

```
定时任务触发 → AiSunoSyncJob → 查询进行中音乐 → 调用SunoApi获取状态 → 更新数据库状态 → 记录日志
```

## API接口说明

### 1. 音乐生成

**接口地址**: `/ai/music/generate`

**请求方法**: POST

**请求参数**:
```json
{
  "platform": "Suno",
  "generateMode": 1,
  "prompt": "创作一首带有轻松吉他旋律的流行歌曲",
  "makeInstrumental": true,
  "model": "chirp-v3.5"
}
```

**响应**: 音乐ID列表

### 2. 获取音乐分页

**接口地址**: `/ai/music/page`

**请求方法**: GET

**请求参数**:
```
userId: 12212
status: 20
publicStatus: true
pageNo: 1
pageSize: 10
```

**响应**: 音乐分页数据

### 3. 更新音乐

**接口地址**: `/ai/music/update`

**请求方法**: PUT

**请求参数**:
```json
{
  "id": 15583,
  "publicStatus": true
}
```

**响应**: 成功标识

### 4. 删除音乐

**接口地址**: `/ai/music/delete`

**请求方法**: DELETE

**请求参数**:
```
id: 15583
```

**响应**: 成功标识

## 关键技术实现

### 1. 音乐生成模式

系统支持两种音乐生成模式：

- **描述模式**: 基于描述词生成音乐，适合快速创作
- **歌词模式**: 基于歌词和音乐风格生成音乐，适合有特定需求的用户

### 2. 音乐状态同步

通过定时任务定期同步第三方平台的音乐生成状态，确保用户能及时了解音乐生成进度。

### 3. 文件上传处理

音乐生成完成后，系统会将音频文件上传到文件服务器，并保存文件地址到数据库中。

### 4. 权限控制

用户只能操作自己的音乐作品，其他用户的音乐对其不可见。

## 配置说明

### Suno配置

在 `application.yml` 中配置Suno API：

```yaml
yudao:
  ai:
    suno:
      enable: true
      base-url: https://api.suno.ai
```

### AI模型配置

系统支持多种AI模型，可以在配置文件中启用或禁用特定的AI模型。

## 与其他模块的关系

### 依赖模块

- **yudao-module-ai**: 核心AI模块，提供AI模型服务
- **yudao-framework**: 基础框架，提供通用服务和工具类
- **File模块**: 文件上传服务，用于保存生成的音乐文件

### 依赖服务

- **AiModelService**: 提供Suno API客户端
- **FileApi**: 文件上传服务

## 部署与运维

### 定时任务配置

系统配置了定时任务 `AiSunoSyncJob` 来同步音乐状态，可以通过Quartz调度器进行配置。

### 监控指标

- 音乐生成成功率
- 音乐生成平均耗时
- 音乐同步成功率

## 未来扩展

1. **支持更多AI音乐平台**: 扩展支持更多第三方音乐生成平台
2. **音乐编辑功能**: 提供在线音乐编辑工具
3. **音乐分享功能**: 支持将音乐分享到社交平台
4. **音乐推荐功能**: 基于用户偏好推荐音乐

## 总结

AI音乐模块为系统提供了强大的AI音乐生成功能，通过集成第三方AI音乐平台，用户可以快速生成高质量的音乐作品。系统设计了完善的音乐管理和同步机制，确保用户能够方便地管理和追踪音乐生成进度。

模块采用了分层架构设计，具有良好的可扩展性和维护性，为系统提供了丰富的AI音乐功能支持。