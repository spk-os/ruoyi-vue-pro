# DBFileClient 模块

## 概述

DBFileClient 模块是 Yudao 框架中基于数据库存储的文件客户端实现。它提供了一种将文件内容存储在数据库中的机制，适用于对文件安全性和一致性要求较高的场景。

该模块继承自 `AbstractFileClient`，实现了 `FileClient` 接口，提供了文件上传、删除和读取的核心功能。

## 核心组件

该模块包含两个核心组件：
1. DBFileClient - 基于数据库存储的文件客户端实现
2. DBFileClientConfig - DBFileClient 的配置类

## 核心功能

### DBFileClient

DBFileClient 是基于数据库存储的文件客户端的主要实现类。它提供了以下核心功能：

1. **文件上传**：将文件内容存储到数据库中的 `infra_file_content` 表
2. **文件删除**：从数据库中删除指定路径的文件记录
3. **文件读取**：从数据库中检索指定路径的文件内容

### DBFileClientConfig

DBFileClientConfig 是 DBFileClient 的配置类，包含以下属性：

- `domain`：自定义域名，用于构建文件的访问URL

## 与其他模块的关系

DBFileClient 模块是文件存储框架的一部分，与其他文件客户端实现（如 LocalFileClient、S3FileClient 等）并行存在。它通过以下方式与系统其他部分集成：

1. 通过 `FileConfigDO` 中的 `config` 字段（使用 `FileClientConfigTypeHandler`）与数据库中的文件配置关联
2. 通过 `FileContentMapper` 与数据库交互，操作 `infra_file_content` 表
3. 实现 `FileClient` 接口，使其可以被文件服务层统一调用

## 使用示例

```java
// 创建 DBFileClient 配置
DBFileClientConfig config = new DBFileClientConfig();
config.setDomain("https://example.com");

// 创建 DBFileClient 实例
DBFileClient dbFileClient = new DBFileClient(1L, config);
dbFileClient.init();

// 上传文件
byte[] fileContent = "Hello, World!".getBytes();
String filePath = dbFileClient.upload(fileContent, "test.txt", "text/plain");

// 读取文件
byte[] content = dbFileClient.getContent("test.txt");

// 删除文件
dbFileClient.delete("test.txt");
```

## 数据库表结构

DBFileClient 使用以下数据库表存储文件内容：

```sql
CREATE TABLE infra_file_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_id BIGINT NOT NULL COMMENT '关联 FileConfigDO 的 ID',
    path VARCHAR(255) NOT NULL COMMENT '文件路径，即文件名',
    content LONGBLOB NOT NULL COMMENT '文件内容'
);
```