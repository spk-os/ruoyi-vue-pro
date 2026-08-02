# AI 模块枚举常量文档

## 1. 概述

本文档主要描述了 AI 模块中使用的枚举常量，特别是字典类型常量。这些枚举常量用于定义 AI 写作、对话、图像生成等功能的参数选项，为系统提供统一的数据字典管理机制。

AI 模块的枚举常量主要集中在 `yudao-module-ai` 模块中，通过 `DictTypeConstants` 接口进行统一管理。

## 2. 架构设计

### 2.1 架构图

AI 模块架构如下所示：

```
AI 模块
├── DictTypeConstants 接口
│   ├── AI_WRITE_FORMAT
│   ├── AI_WRITE_LENGTH
│   ├── AI_WRITE_LANGUAGE
│   └── AI_WRITE_TONE
├── AI 服务层
│   ├── AiWriteServiceImpl
│   ├── AiChatServiceImpl
│   ├── AiImageServiceImpl
│   └── AiKnowledgeServiceImpl
├── 字典服务
│   └── DictFrameworkUtils
├── 前端应用
│   └── API 接口
└── AI 模型服务
    └── 第三方 AI 平台
```
### 2.2 组件关系

- **DictTypeConstants**: 定义 AI 模块中所有字典类型的常量
- **AI 服务层**: 提供 AI 功能的具体实现
- **字典服务**: 提供字典数据查询和管理功能
- **前端应用**: 通过 API 接口调用 AI 服务
- **AI 模型服务**: 调用第三方 AI 平台生成内容

## 3. 核心枚举常量详解

### 3.1 DictTypeConstants 接口

位于: `yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/enums/DictTypeConstants.java`

```java
package cn.iocoder.yudao.module.ai.enums;

/**
 * AI 字典类型的枚举类
 *
 * @author xiaoxin
 */
public interface DictTypeConstants {

    // ========== AI Write ==========
    String AI_WRITE_FORMAT = "ai_write_format"; // 写作格式
    String AI_WRITE_LENGTH = "ai_write_length"; // 写作长度
    String AI_WRITE_LANGUAGE = "ai_write_language"; // 写作语言
    String AI_WRITE_TONE = "ai_write_tone"; // 写作语气

}
```

### 3.2 字典类型常量说明

| 常量名称 | 字典类型值 | 说明 | 使用场景 |
|---------|-----------|------|----------|
| AI_WRITE_FORMAT | ai_write_format | 写作格式 | 控制 AI 输出内容的格式，如：文章、报告、邮件等 |
| AI_WRITE_LENGTH | ai_write_length | 写作长度 | 控制 AI 输出内容的长度，如：短文、中等、长文等 |
| AI_WRITE_LANGUAGE | ai_write_language | 写作语言 | 控制 AI 输出内容的语言，如：中文、英文、日文等 |
| AI_WRITE_TONE | ai_write_tone | 写作语气 | 控制 AI 输出内容的语气，如：正式、随意、幽默等 |

## 4. 使用示例

### 4.1 前端调用示例

前端通过 API 接口调用 AI 写作服务时，需要传递格式、长度、语言、语气等参数，这些参数值对应于字典数据中的键值。

```typescript
// 示例：AI 写作请求参数
const generateReq = {
  type: 1, // 写作类型
  prompt: '写一篇关于人工智能的文章',
  format: 1, // 文章格式
  length: 2, // 中等长度
  tone: 1,   // 正式语气
  language: 1 // 中文
};
```

### 4.2 后端服务使用示例

在 `AiWriteServiceImpl` 中，使用 `DictFrameworkUtils` 解析字典数据标签：

```java
@Service
@Slf4j
public class AiWriteServiceImpl implements AiWriteService {

    @Override
    public Flux<CommonResult<String>> generateWriteContent(AiWriteGenerateReqVO generateReqVO, Long userId) {
        // ...
        
        // 使用 DictFrameworkUtils 解析字典数据标签
        String format = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_FORMAT, generateReqVO.getFormat());
        String tone = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_TONE, generateReqVO.getTone());
        String language = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_LANGUAGE, generateReqVO.getLanguage());
        String length = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_LENGTH, generateReqVO.getLength());
        
        // 构建用户消息
        String prompt = generateReqVO.getPrompt();
        if (Objects.equals(generateReqVO.getType(), AiWriteTypeEnum.WRITING.getType())) {
            return StrUtil.format(AiWriteTypeEnum.WRITING.getPrompt(), prompt, format, tone, language, length);
        } else {
            return StrUtil.format(AiWriteTypeEnum.REPLY.getPrompt(), generateReqVO.getOriginalContent(), prompt, format, tone, language, length);
        }
    }
}
```

### 4.3 API 接口定义

`AiWriteController` 提供了写作相关的 API 接口：

```java
@RestController
@RequestMapping("/ai/write")
public class AiWriteController {

    @Resource
    private AiWriteService writeService;

    @PostMapping(value = "/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "写作生成（流式）", description = "流式返回，响应较快")
    public Flux<CommonResult<String>> generateWriteContent(@RequestBody @Valid AiWriteGenerateReqVO generateReqVO) {
        return writeService.generateWriteContent(generateReqVO, getLoginUserId());
    }

    // ... 其他接口方法
}
```

## 5. 字典数据管理

### 5.1 字典数据结构

字典数据通常存储在数据库中，包含以下字段：
- `dict_type`: 字典类型，对应 DictTypeConstants 中的常量值
- `value`: 字典键值
- `label`: 字典标签（显示值）
- `sort`: 排序
- `status`: 状态
- `remark`: 备注

### 5.2 字典数据示例

对于 `ai_write_format` 字典类型，可能的数据如下：

| dict_type | value | label | sort | status | remark |
|-----------|-------|-------|------|--------|--------|
| ai_write_format | 1 | 文章 | 1 | 0 | 标准文章格式 |
| ai_write_format | 2 | 报告 | 2 | 0 | 正式报告格式 |
| ai_write_format | 3 | 邮件 | 3 | 0 | 邮件格式 |

## 6. 扩展性设计

### 6.1 添加新的字典类型

要添加新的字典类型，需要：

1. 在 `DictTypeConstants` 接口中添加新的常量定义
2. 在数据库中添加对应的字典数据
3. 在使用的服务中调用 `DictFrameworkUtils.parseDictDataLabel()` 解析字典标签

### 6.2 示例：添加新的写作风格

```java
// 1. 在 DictTypeConstants 中添加新常量
public interface DictTypeConstants {
    // ... 现有常量
    String AI_WRITE_STYLE = "ai_write_style"; // 写作风格
}

// 2. 在数据库中添加对应的字典数据
// dict_type: ai_write_style
// value: 1, label: 诗歌, sort: 1
// value: 2, label: 散文, sort: 2
// value: 3, label: 小说, sort: 3

// 3. 在 AiWriteServiceImpl 中使用新字典类型
private String buildUserMessage(AiWriteGenerateReqVO generateReqVO) {
    String format = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_FORMAT, generateReqVO.getFormat());
    String tone = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_TONE, generateReqVO.getTone());
    String language = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_LANGUAGE, generateReqVO.getLanguage());
    String length = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_LENGTH, generateReqVO.getLength());
    String style = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_STYLE, generateReqVO.getStyle()); // 新增
    
    // ... 构建 Prompt
}
```

## 7. 与其他模块的集成

### 7.1 与系统模块的集成

AI 模块的字典类型常量遵循系统模块的统一规范，与系统字典服务无缝集成。

### 7.2 与前端的集成

前端通过 API 接口获取字典数据，并使用字典标签值进行显示和交互。

## 8. 最佳实践

1. **统一管理**: 所有字典类型常量应集中在 `DictTypeConstants` 接口中定义，避免分散定义
2. **清晰命名**: 常量命名应清晰、有意义，便于理解和维护
3. **文档化**: 每个常量应包含详细的注释说明
4. **扩展性**: 设计时应考虑未来的扩展需求，预留足够的接口和参数空间
5. **一致性**: 遵循系统统一的字典数据管理规范

## 9. 参考文档

- [系统字典服务文档](../yudao-framework/yudao-spring-boot-starter-excel.md)
- [AI 写作服务文档](ai-write.md)
- [AI 对话服务文档](ai-chat.md)
- [AI 图像生成服务文档](ai-image.md)