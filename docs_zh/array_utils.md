# ArrayUtils 工具类

## 模块概述

ArrayUtils 是 Yudao 框架中用于数组操作的工具类，位于 `cn.iocoder.yudao.framework.common.util.collection` 包下。该工具类提供了数组与集合之间的转换、数组元素追加以及安全获取数组元素等实用功能。

该工具类依赖于 HuTool 库的 ArrayUtil 和 CollectionUtil 类，以及框架自身的 CollectionUtils 工具类，提供了类型安全且易于使用的数组操作方法。

## 架构概述

ArrayUtils 作为集合工具包的一部分，与其他集合工具类协同工作，为框架各层提供统一的集合和数组操作能力。

### 模块位置
```
yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/util/collection/
├── ArrayUtils.java          # 本文档描述的类
├── CollectionUtils.java     # 集合工具类（被 ArrayUtils 依赖）
├── MapUtils.java            # Map 工具类
└── SetUtils.java            # Set 工具类
```

### 依赖关系
```
ArrayUtils --> CollectionUtils
ArrayUtils --> HuTool.ArrayUtil
ArrayUtils --> HuTool.CollectionUtil
ArrayUtils --> HuTool.IterUtil
CollectionUtils --> HuTool.CollectionUtil
CollectionUtils --> HuTool.IterUtil
```

## 功能详解

### 核心方法

#### 1. append 方法
```java
@SafeVarargs
public static <T> Consumer<T>[] append(Consumer<T> object, Consumer<T>... newElements)
```
将单个对象和多个新元素合并成一个 Consumer 数组。

**参数:**
- `object`: 要添加到数组开头的对象（可以为 null）
- `newElements`: 要追加的元素数组

**返回值:** 包含原对象和新元素的 Consumer 数组

**使用示例:**
```java
Consumer<String> printer = s -> System.out.println(s);
Consumer<String> logger = s -> logger.info(s);
Consumer<String>[] consumers = ArrayUtils.append(printer, logger, s -> System.err.println(s));
// consumers[0] = printer, consumers[1] = logger, consumers[2] = s -> System.err.println(s)
```

#### 2. toArray 方法（带映射函数）
```java
public static <T, V> V[] toArray(Collection<T> from, Function<T, V> mapper)
```
将集合转换为指定类型的数组，使用提供的映射函数转换元素。

**参数:**
- `from`: 源集合
- `mapper`: 元素映射函数

**返回值:** 映射后的数组

**使用示例:**
```java
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
Integer[] lengths = ArrayUtils.toArray(names, String::length);
// lengths = [5, 3, 7]
```

#### 3. toArray 方法（不带映射函数）
```java
@SuppressWarnings("unchecked")
public static <T> T[] toArray(Collection<T> from)
```
将集合转换为同类型数组，自动推断元素类型。

**参数:**
- `from`: 源集合

**返回值:** 包含集合所有元素的数组，如果集合为空则返回空 Object 数组

**使用示例:**
```java
List<Integer> numbers = Arrays.asList(1, 2, 3);
Integer[] numbersArray = ArrayUtils.toArray(numbers);
// numbersArray = [1, 2, 3]

List<String> emptyList = Collections.emptyList();
Object[] emptyArray = ArrayUtils.toArray(emptyList);
// emptyArray = []
```

#### 4. get 方法
```java
public static <T> T get(T[] array, int index)
```
安全地获取数组中指定索引的元素，避免数组越界异常。

**参数:**
- `array`: 目标数组（可以为 null）
- `index`: 要获取的元素索引

**返回值:** 指定索引处的元素，如果数组为 null 或索引越界则返回 null

**使用示例:**
```java
String[] names = {"Alice", "Bob"};
String first = ArrayUtils.get(names, 0);    // 返回 "Alice"
String third = ArrayUtils.get(names, 2);    // 返回 null（索引越界）
String nullArray = ArrayUtils.get(null, 0); // 返回 null（数组为 null）
```

## 使用场景

### 场景 1：事件监听器链构建
在事件驱动架构中，经常需要将多个事件处理器组合成一个处理器链：

```java
// 定义事件处理器
Consumer<UserEvent> logHandler = event -> logger.info("User event: {}", event);
Consumer<UserEvent> auditHandler = event -> auditService.log(event);
Consumer<UserEvent> notificationHandler = event -> notificationService.send(event);

// 创建处理器链
Consumer<UserEvent>[] handlers = ArrayUtils.append(
    logHandler, 
    auditHandler, 
    notificationHandler
);

// 执行所有处理器
for (Consumer<UserEvent> handler : handlers) {
    handler.accept(userEvent);
}
```

### 场景 2：DTO 转换
将业务对象集合转换为数据传输对象数组用于 API 响应：

```java
// 从服务层获取用户列表
List<User> users = userService.getActiveUsers();

// 转换为用户DTO数组返回给前端
UserDTO[] userDtos = ArrayUtils.toArray(users, user -> {
    UserDTO dto = new UserDTO();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    return dto;
});

// 返回给前端
return ResponseEntity.ok(userDtos);
```

### 场景 3：安全数组访问
在处理可能为空或索引越界的数组时，使用 get 方法避免空指针异常：

```java
public String getUserNameById(String[] userIds, int index) {
    String userId = ArrayUtils.get(userIds, index);
    if (userId == null) {
        return "unknown";
    }
    return userRepository.findNameById(userId);
}
```

## 与其他工具类的关系

ArrayUtils 与其他集合工具类形成互补关系：

| 工具类 | 主要功能 | 与 ArrayUtils 的关系 |
|--------|----------|---------------------|
| ArrayUtils | 数组操作 | 提供数组与集合之间的转换 |
| CollectionUtils | 集合操作 | 提供集合转换等功能被 ArrayUtils 使用 |
| MapUtils | Map 操作 | 互补，处理不同集合类型 |
| SetUtils | Set 操作 | 互补，处理不同集合类型 |

## 实现细节

### 依赖库
ArrayUtils 依赖以下工具库：
- **HuTool ArrayUtil**: 提供数组创建和转换基础功能
- **HuTool CollectionUtil**: 用于集合操作
- **HuTool IterUtil**: 用于获取迭代器元素类型
- **框架 CollectionUtils**: 提供集合转换功能

### 类型安全考虑
- 使用泛型确保类型安全
- 在 `toArray(Collection<T>)` 方法中使用 `@SuppressWarnings("unchecked")` 注解来处理泛型数组创建的警告
- 通过 `IterUtil.getElementType()` 安全获取集合元素类型

### 性能考虑
- 使用系统数组复制 (`System.arraycopy`) 实现高效的数组合并
- 集合转换操作利用 Java Stream API 实现简洁高效的处理
- 空集合检查提前返回，避免不必要的处理

## 最佳实践

1. **优先使用类型安全方法**：优先使用带映射函数的 `toArray(Collection<T>, Function<T, V>)` 方法以确保类型安全
2. **处理空值情况**：所有方法都妥善处理了 null 输入情况
3. **避免强制类型转换**：通过泛型设计减少强制类型转换的需求
4. **利用现有工具**：充分利用 HuTool 和框架自身的工具类减少重复实现

## 性能特点

| 操作 | 时间复杂度 | 空间复杂度 | 说明 |
|------|------------|------------|------|
| append | O(n) | O(n) | 需要创建新数组并复制元素 |
| toArray (with mapper) | O(n) | O(n) | 需要遍历集合并应用映射函数 |
| toArray (without mapper) | O(n) | O(n) | 需要遍历集合并转换为数组 |
| get | O(1) | O(1) | 直接数组索引访问 |

## 版本历史

- **版本 1.0.0**: 初始版本，提供基本的数组操作功能
- **当前版本**: 维持向后兼容性，持续优化性能和可用性

## 与其他模块的集成

ArrayUtils 被框架各个模块广泛使用，特别是在：
- 服务层：DTO 转换和数据处理
- 控制器层：请求参数处理和响应构建
- 工具类：各种需要数组操作的辅助功能

例如，在用户模块中可能会看到：
```java
// 将用户ID列表转换为数组用于批量查询
Long[] userIds = ArrayUtils.toArray(userIdList, Function.identity());
// 使用数组进行数据库批量操作
List<User> users = userMapper.selectByIds(userIds);
```

## 注意事项

1. 类型擦除：由于 Java 泛型擦除，`toArray(Collection<T>)` 方法返回的实际类型是 Object[]，需要进行适当的类型转换
2. null 处理：所有方法都安全处理 null 输入，但返回值可能为 null（特别是 get 方法）
3. 性能考虑：对于大规模数据处理，考虑使用原始数组操作而非频繁的集合-数组转换
4. 异常情况：get 方法在索引越界或数组为 null 时返回 null，而不是抛出异常

## 结论

ArrayUtils 是一个实用的工具类，提供了安全、类型安全的数组操作方法。它通过封装常见的数组操作模式，减少了样板代码，提高了代码可读性和可维护性。与框架其他集合工具类配合使用，能够为开发者提供完整的集合和数组处理解决方案。