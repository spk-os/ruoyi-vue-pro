# vo_3 模块文档

## 模块概述

vo_3 模块位于 `yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/controller/admin/music/vo/`，是 AI 音乐功能在管理后台的值对象（Value Object）集合。该模块定义了 AI 音乐相关的请求和响应数据传输对象（DTO），用于前后端交互中的数据封装和验证。

## 架构概述

vo_3 模块是 AI 音乐功能的数据传输层，主要包含以下几类对象：
- 请求对象（Request VO）：用于接收前端提交的数据
- 响应对象（Response VO）：用于向前端返回处理后的数据
- 分页请求对象（PageReqVO）：用于分页查询
- 特定功能请求对象（如 Suno 生成请求）：用于特定业务场景

这些对象通过控制器层（controller）与服务层（service）进行交互，但本模块仅关注数据传输对象的定义。

![vo_3模块架构图](https://via.placeholder.com/800x400?text=vo_3+Module+Architecture)
*图：vo_3 模块架构概览*

## 核心组件说明

### AiMusicUpdateReqVO
**文件路径**：`yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/controller/admin/music/vo/AiMusicUpdateReqVO.java`

用于管理后台修改 AI 音乐信息的请求对象。

| 属性 | 类型 | 说明 | 备注 |
|------|------|------|------|
| id | Long | 音乐编号 | 必填，示例值：15583 |
| publicStatus | Boolean | 是否发布 | 可选，示例值：true |

### AiMusicRespVO
**文件路径**：`yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/controller/admin/music/vo/AiMusicRespVO.java`

用于管理后台返回 AI 音乐信息的响应对象，包含音乐的完整详情。

| 属性 | 类型 | 说明 | 备注 |
|------|------|------|------|
| id | Long | 编号 | 必填，示例值：24790 |
| userId | Long | 用户编号 | 必填，示例值：12212 |
| title | String | 音乐名称 | 必填，示例值：夜空中最亮的星 |
| lyric | String | 歌词 | 可选，示例值：oh~卖糕的 |
| imageUrl | String | 图片地址 | 可选，示例值：https://www.iocoder.cn |
| audioUrl | String | 音频地址 | 可选，示例值：https://www.iocoder.cn |
| videoUrl | String | 视频地址 | 可选，示例值：https://www.iocoder.cn |
| status | Integer | 音乐状态 | 必填，示例值：20 |
| gptDescriptionPrompt | String | 描述词 | 可选，示例值：一首轻快的歌曲 |
| prompt | String | 提示词 | 可选，示例值：创作一首带有轻松吉他旋律的流行歌曲，[verse] 描述夏日海滩的宁静，[chorus] 节奏加快，表达对自由的向往。 |
| platform | String | 模型平台 | 必填，示例值：Suno |
| model | String | 模型 | 必填，示例值：chirp-v3.5 |
| generateMode | Integer | 生成模式 | 必填，示例值：1 |
| tags | List<String> | 音乐风格标签 | 可选 |
| duration | Double | 音乐时长 | 可选，示例值：["pop","jazz","punk"]（注意：此处注释有误，实际应为数值） |
| publicStatus | Boolean | 是否发布 | 必填，示例值：true |
| taskId | String | 任务编号 | 可选，示例值：11369 |
| errorMessage | String | 错误信息 | 可选 |
| createTime | LocalDateTime | 创建时间 | 必填 |

### AiMusicUpdateMyReqVO
**文件路径**：`yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/controller/admin/music/vo/AiMusicUpdateMyReqVO.java`

用于管理后台修改当前用户的 AI 音乐信息的请求对象。

| 属性 | 类型 | 说明 | 备注 |
|------|------|------|------|
| id | Long | 音乐编号 | 必填，示例值：15583 |
| title | String | 音乐名称 | 可选，示例值：夜空中最亮的星 |

### AiMusicPageReqVO
**文件路径**：`yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/controller/admin/music/vo/AiMusicPageReqVO.java`

用于管理后台分页查询 AI 音乐的请求对象，继承自通用分页参数。

| 属性 | 类型 | 说明 | 备注 |
|------|------|------|------|
| userId | Long | 用户编号 | 可选，示例值：12212 |
| title | String | 音乐名称 | 可选，示例值：夜空中最亮的星 |
| status | Integer | 音乐状态 | 可选，示例值：20（需在 AiMusicStatusEnum 枚举范围内） |
| generateMode | Integer | 生成模式 | 可选，示例值：1（需在 AiMusicGenerateModeEnum 枚举范围内） |
| publicStatus | Boolean | 是否发布 | 可选，示例值：true |
| createTime | LocalDateTime[] | 创建时间范围 | 可选，格式：yyyy-MM-dd HH:mm:ss |

### AiSunoGenerateReqVO
**文件路径**：`yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/controller/admin/music/vo/AiSunoGenerateReqVO.java`

用于管理后台通过 Suno 平台生成 AI 音乐的请求对象。

| 属性 | 类型 | 说明 | 备注 |
|------|------|------|------|
| platform | String | 平台 | 必填，示例值：Suno（参见 AiPlatformEnum 枚举） |
| generateMode | Integer | 生成模式 | 必填，示例值：2（参见 AiMusicGenerateModeEnum 枚举）<br>1. 描述模式：描述词 + 是否纯音乐 + 模型<br>2. 歌词模式：歌词 + 音乐风格 + 标题 + 模型 |
| prompt | String | 用于生成音乐音频的歌词提示 | 可选，示例值：多行歌词内容 |
| makeInstrumental | Boolean | 是否纯音乐 | 可选，示例值：true |
| model | String | 模型 | 必填，示例值：chirp-v3.5（非空） |
| tags | List<String> | 音乐风格 | 可选，示例值：["pop","jazz","punk"] |
| title | String | 音乐/歌曲名称 | 可选，示例值：夜空中最亮的星 |

## 数据流说明

在 AI 音乐功能中，这些 VO 的典型使用流程如下：

1. 前端通过 `AiMusicPageReqVO` 发送分页查询请求
2. 后端返回 `AiMusicRespVO` 列表作为响应
3. 前端通过 `AiMusicUpdateReqVO` 或 `AiMusicUpdateMyReqVO` 发送修改请求
4. 前端通过 `AiSunoGenerateReqVO` 发送音乐生成请求
5. 后端处理后返回相应的响应对象（通常为成功状态或包含生成结果的 VO）

![vo_3数据流图](https://via.placeholder.com/800x400?text=vo_3+Data+Flow)
*图：vo_3 模块数据流示例*

## 与其他模块的关系

vo_3 模块主要服务于 AI 音乐功能的控制器层，与以下模块交互：
- **服务层**（service）：如 `AiMusicServiceImpl`，处理业务逻辑
- **数据访问层**（dal）：操作数据库持久化
- **其他 VO 模块**：如音乐相关的实体对象（DO）和枚举类

在系统架构中，vo_3 属于表现层（presentation layer）的数据传输对象定义，不直接处理业务逻辑，但为控制器和服务层之间的通信提供了类型安全的数据结构。

## 使用指南

### 在控制器中的使用示例
```java
@RestController
@RequestMapping("/ai/music")
@Validated
public class AiMusicController {

    @PostMapping("/update")
    public CommonResult<Void> updateMusic(@Valid @RequestBody AiMusicUpdateReqVO updateReqVO) {
        // 调用 service 处理更新逻辑
        return CommonResult.success();
    }

    @GetMapping("/page")
    public CommonResult<PageResult<AiMusicRespVO>> getMusicPage(@Valid AiMusicPageReqVO pageReqVO) {
        // 调用 service 处理分页查询
        return CommonResult.success(pageResult);
    }

    @PostMapping("/suno/generate")
    public CommonResult<String> generateSunoMusic(@Valid @RequestBody AiSunoGenerateReqVO generateReqVO) {
        // 调用 service 处理 Suno 生成逻辑
        return CommonResult.success(taskId);
    }
}
```

### 验证说明
- 使用 `@NotNull`、`@NotBlank`、`@NotEmpty` 等注解进行字段非空验证
- 使用 `@InEnum` 注解验证枚举字段的合法性
- 使用 `@Schema` 注解提供 Swagger 文档描述

## 依赖关系

vo_3 模块依赖以下基础组件：
- Lombok：用于自动生成 getter、setter 等方法
- Spring Validation：用于参数验证
- Swagger/OpenAPI：用于 API 文档生成
- JDK 8+ 时间日期 API：使用 `LocalDateTime`

## 结论

vo_3 模块为 AI 音乐功能提供了统一的数据传输对象定义，确保了前后端数据交互的一致性和类型安全。通过清晰的字段定义和验证规则，提高了系统的健壮性和可维护性。该模块与 AI 音乐功能的其他层（控制器、服务、数据访问）协同工作，构建了完整的音乐管理功能链路。