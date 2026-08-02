# MapUtils 模块文档

## 概述

MapUtils 是 Yudao 框架中用于 Map 操作的工具类，提供了一系列实用的静态方法来简化 Map 的常见操作。该类位于 `cn.iocoder.yudao.framework.common.util.collection` 包下，是集合工具包的一部分，与 CollectionUtils、ArrayUtils 和 SetUtils 协同工作。

## 核心功能

MapUtils 提供以下核心功能：

1. **从 Multimap 获取值列表**：根据键集合从 Multimap 中获取对应的所有值
2. **查找并处理 Map 值**：安全地从 Map 中获取值并执行自定义处理逻辑
3. **KeyValue 列表转 Map**：将 KeyValue 对象列表转换为 Map
4. **安全获取 BigDecimal 值**：从 Map 中获取 BigDecimal 类型值，支持自动类型转换和默认值处理

## 详细API说明

### getList 方法

```java
public static <K, V> List<V> getList(Multimap<K, V> multimap, Collection<K> keys)
```

从哈希表中，获得 keys 对应的所有 value 数组。

**参数：**
- `multimap`: 哈希表（Multimap 实现）
- `keys`: 键集合

**返回值：** 包含所有对应值的列表

**使用示例：**
```java
Multimap<String, String> multimap = ArrayListMultimap.create();
multimap.put("key1", "value1");
multimap.put("key1", "value2");
multimap.put("key2", "value3");

List<String> result = MapUtils.getList(multimap, Arrays.asList("key1", "key2"));
// result 包含 ["value1", "value2", "value3"]
```

### findAndThen 方法

```java
public static <K, V> void findAndThen(Map<K, V> map, K key, Consumer<V> consumer)
```

从哈希表查找到 key 对应的 value，然后进一步处理。key 为 null 时, 不处理。注意，如果查找到的 value 为 null 时，不进行处理。

**参数：**
- `map`: 哈希表
- `key`: 要查找的键
- `consumer`: 对找到的值进行进一步处理的消费者函数

**使用示例：**
```java
Map<String, String> map = new HashMap<>();
map.put("name", "Yudao");

MapUtils.findAndThen(map, "name", value -> {
    System.out.println("Name value: " + value);
    // 执行其他处理逻辑
});
```

### convertMap 方法

```java
public static <K, V> Map<K, V> convertMap(List<KeyValue<K, V>> keyValues)
```

将 KeyValue 对象列表转换为 Map。

**参数：**
- `keyValues`: KeyValue 对象列表

**返回值：** 转换后的 Map

**使用示例：**
```java
List<KeyValue<String, Integer>> keyValues = Arrays.asList(
    new KeyValue<>("apple", 5),
    new KeyValue<>("banana", 3),
    new KeyValue<>("orange", 8)
);

Map<String, Integer> fruitMap = MapUtils.convertMap(keyValues);
// fruitMap 包含 {"apple": 5, "banana": 3, "orange": 8}
```

### getBigDecimal 方法 (无默认值)

```java
public static BigDecimal getBigDecimal(Map<String, ?> map, String key)
```

从 Map 中获取 BigDecimal 值。

**参数：**
- `map`: Map 数据源
- `key`: 键名

**返回值：** BigDecimal 值，解析失败或值为 null 时返回 null

**使用示例：**
```java
Map<String, Object> data = new HashMap<>();
data.put("price", "123.45");
data.put("amount", 99);
data.put("exact", new BigDecimal("0.01"));

BigDecimal price = MapUtils.getBigDecimal(data, "price");    // 123.45
BigDecimal amount = MapUtils.getBigDecimal(data, "amount");  // 99
BigDecimal exact = MapUtils.getBigDecimal(data, "exact");    // 0.01
BigDecimal missing = MapUtils.getBigDecimal(data, "missing"); // null
```

### getBigDecimal 方法 (带默认值)

```java
public static BigDecimal getBigDecimal(Map<String, ?> map, String key, BigDecimal defaultValue)
```

从 Map 中获取 BigDecimal 值。

**参数：**
- `map`: Map 数据源
- `key`: 键名
- `defaultValue`: 默认值

**返回值：** BigDecimal 值，解析失败或值为 null 时返回默认值

**使用示例：**
```java
Map<String, Object> data = new HashMap<>();
data.put("price", "123.45");
data.put("invalid", "not a number");

BigDecimal price = MapUtils.getBigDecimal(data, "price", BigDecimal.ZERO);    // 123.45
BigDecimal invalid = MapUtils.getBigDecimal(data, "invalid", BigDecimal.ZERO); // 0 (默认值)
BigDecimal missing = MapUtils.getBigDecimal(data, "missing", BigDecimal.ZERO); // 0 (默认值)
```

## 与其他工具类的关系

MapUtils 是集合工具包的一部分，与以下工具类协同工作：

- **CollectionUtils**：提供集合操作的工具方法
- **ArrayUtils**：提供数组操作的工具方法
- **SetUtils**：提供 Set 操作的工具方法

这些工具类共同构成了 Yudao 框架的集合操作工具集，为开发者提供了全面的集合处理能力。

## 使用场景

MapUtils 适用于以下场景：

1. **处理多值映射**：当需要根据多个键从 Multimap 中获取所有对应值时
2. **安全的 Map 值处理**：当需要从 Map 中获取值并执行自定义处理，同时避免空指针异常时
3. **数据转换**：将 KeyValue 对象列表转换为 Map 结构
4. **类型安全的数值获取**：从 Map 中安全地获取 BigDecimal 值，支持字符串、数值等多种类型的自动转换

## 依赖关系

MapUtils 依赖以下外部库：
- Google Guava（Multimap, Maps）
- Hutool（CollectionUtil, ObjUtil）
- Yudao 框架自身的 KeyValue 类

## 最佳实践

1. 使用 `findAndThen` 方法可以避免空指针检查的样板代码
2. 在处理可能包含不同数值类型的 Map 时，使用 `getBigDecimal` 方法可以确保类型安全
3. 将键值对列表转换为 Map 时，使用 `convertMap` 方法比手动遍历更简洁高效
4. 处理 Multimap 时，使用 `getList` 方法可以一次性获取多个键的所有值

## 版本信息

- 作者：芋道源码
- 所属包：cn.iocoder.yudao.framework.common.util.collection
- 相关文件：CollectionUtils.java, ArrayUtils.java, SetUtils.java