# BeanUtils 模块文档

## 概述

BeanUtils 是 Yudao 框架中的一个工具类，提供了基于 Hutool BeanUtil 的 bean 转换和属性复制功能。它封装了常见的对象转换场景，包括单对象转换、列表转换和分页结果转换，并支持转换后的后置处理。

## 核心功能

BeanUtils 提供以下核心功能：

1. **单对象转换**：将源对象转换为目标类型的对象
2. **列表转换**：将源对象列表转换为目标类型的对象列表
3. **分页结果转换**：将包含列表的分页结果转换为目标类型的分页结果
4. **属性复制**：将源对象的属性复制到目标对象


### 单对象转换

#### toBean(Object source, Class<T> targetClass)
将源对象转换为指定目标类型的对象。

**参数：**
- `source`: 源对象，可以为 null
- `targetClass`: 目标类类型

**返回值：** 转换后的目标类型对象，如果源对象为 null 则返回 null

**示例：**
```java
UserVO userVO = BeanUtils.toBean(userDO, UserVO.class);
```

#### toBean(Object source, Class<T> targetClass, Consumer<T> peek)
将源对象转换为指定目标类型的对象，并支持后置处理。

**参数：**
- `source`: 源对象，可以为 null
- `targetClass`: 目标类类型
- `peek`: 转换后的后置处理函数，可以为 null

**返回值：** 转换后的目标类型对象，如果源对象为 null 则返回 null

**示例：**
```java
UserVO userVO = BeanUtils.toBean(userDO, UserVO.class, vo -> {
    vo.setPassword("******"); // 脱敏处理
});
```

### 列表转换

#### toBean(List<S> source, Class<T> targetType)
将源对象列表转换为目标类型的对象列表。

**参数：**
- `source`: 源对象列表，可以为 null
- `targetType`: 目标类类型

**返回值：** 转换后的目标类型对象列表，如果源列表为 null 则返回 null

**示例：**
```java
List<UserVO> userVOs = BeanUtils.toBean(userDOList, UserVO.class);
```

#### toBean(List<S> source, Class<T> targetType, Consumer<T> peek)
将源对象列表转换为目标类型的对象列表，并支持对每个元素的后置处理。

**参数：**
- `source`: 源对象列表，可以为 null
- `targetType`: 目标类类型
- `peek`: 每个转换后对象的后置处理函数，可以为 null

**返回值：** 转换后的目标类型对象列表，如果源列表为 null 则返回 null

**示例：**
```java
List<UserVO> userVOs = BeanUtils.toBean(userDOList, UserVO.class, vo -> {
    vo.setCreateTime(DateUtils.formatDateTime(vo.getCreateTime()));
});
```

### 分页结果转换

#### toBean(PageResult<S> source, Class<T> targetType)
将源分页结果转换为目标类型的分页结果。

**参数：**
- `source`: 源分页结果，可以为 null
- `targetType`: 目标类类型

**返回值：** 转换后的目标类型分页结果，如果源分页结果为 null 则返回 null

**示例：**
```java
PageResult<UserVO> userVOPage = BeanUtils.toBean(userDOPage, UserVO.class);
```

#### toBean(PageResult<S> source, Class<T> targetType, Consumer<T> peek)
将源分页结果转换为目标类型的分页结果，并支持对列表中每个元素的后置处理。

**参数：**
- `source`: 源分页结果，可以为 null
- `targetType`: 目标类类型
- `peek`: 每个转换后对象的后置处理函数，可以为 null

**返回值：** 转换后的目标类型分页结果，如果源分页结果为 null 则返回 null

**示例：**
```java
PageResult<UserVO> userVOPage = BeanUtils.toBean(userDOPage, UserVO.class, vo -> {
    vo.setStatusDesc(DictUtils.getDictLabel("user_status", vo.getStatus()));
});
```

### 属性复制

#### copyProperties(Object source, Object target)
将源对象的属性复制到目标对象。

**参数：**
- `source`: 源对象，如果为 null 则不执行任何操作
- `target`: 目标对象，如果为 null 则不执行任何操作

**说明：** 此方法使用 Hutool 的 BeanUtil.copyProperties 方法，忽略 null 值属性（第三个参数为 false）

**示例：**
```java
UserDO userDO = new UserDO();
BeanUtils.copyProperties(userVO, userDO); // 将 VO 的属性复制到 DO
```

## 使用场景

### DTO 转换
在服务层和控制层之间进行数据传输对象（DTO）转换时：

```java
// 服务层方法
public UserVO getUserById(Long id) {
    UserDO userDO = userMapper.selectById(id);
    return BeanUtils.toBean(userDO, UserVO.class);
}

// 批量查询
public PageResult<UserVO> getUserPage(UserPageReqVO reqVO) {
    PageResult<UserDO> userDOPage = userMapper.selectPage(reqVO);
    return BeanUtils.toBean(userDOPage, UserVO.class);
}
```

### 数据脱敏
在返回前对敏感字段进行处理：

```java
public UserVO getUserDetail(Long id) {
    UserDO userDO = userMapper.selectById(id);
    return BeanUtils.toBean(userDO, UserVO.class, vo -> {
        vo.setIdNumber(IdCardUtils.desensitizeIdCard(vo.getIdNumber()));
        vo.setMobile(PhoneUtils.desensitizeMobilePhone(vo.getMobile()));
    });
}
```

### 属性复制场景
在需要将一个对象的属性复制到另一个对象时：

```java
// 更新场景
public void updateUser(UserUpdateReqVO reqVO) {
    UserDO userDO = new UserDO();
    BeanUtils.copyProperties(reqVO, userDO); // 复制需要更新的属性
    userMapper.updateById(userDO);
}
```



## 性能说明

1. BeanUtils 基于 Hutool 的 BeanUtil 实现，性能足以满足大多数业务场景
2. 对于高频、高并发的对象转换场景，建议使用 MapStruct 等编译时生成的映射器
3. 框架中已经提供了如 AuthConvert 这样的示例，展示了如何结合 MapStruct 进行复杂对象转换

## 最佳实践

1. **简单转换直接使用**：对于属性名和类型完全匹配的简单转换，直接使用 BeanUtils.toBean()
2. **复杂转换使用 MapStruct**：当需要进行类型转换、格式化或复杂映射时，考虑使用 MapStruct
3. **后置处理利用 peek 参数**：利用带 Consumer 参数的方法进行转换后的统一处理（如脱敏、格式化）
4. **注意 null 安全**：所有转换方法都正确处理了 null 输入情况
5. **避免过度使用**：在性能极其关键的路径中，考虑手动赋值或使用更轻量级的方案

## 示例代码

### 基础用法
```java
// 单对象转换
RoleVO roleVO = BeanUtils.toBean(roleDO, RoleVO.class);

// 列表转换
List<RoleVO> roleVOs = BeanUtils.toBean(roleDOList, RoleVO.class);

// 分页转换
PageResult<RoleVO> roleVOPage = BeanUtils.toBean(roleDOPage, RoleVO.class);
```

### 带后置处理
```java
// 脱敏处理
UserVO userVO = BeanUtils.toBean(userDO, UserVO.class, vo -> {
    vo.setIdNumber(SecurityUtils.desensitizeIdCard(vo.getIdNumber()));
    vo.setEmail(SecurityUtils.desensitizeEmail(vo.getEmail()));
});

// 格式化处理
List<OrderVO> orderVOs = BeanUtils.toBean(orderDOList, OrderVO.class, vo -> {
    vo.setCreateTime(DateUtils.formatDateTime(vo.getCreateTime()));
    vo.setAmountStr(NumberUtils.formatCurrency(vo.getAmount()));
});
```

### 属性复制
```java
// 创建对象时复制属性
UserDO userDO = new UserDO();
BeanUtils.copyProperties(userRegisterVO, userDO);

// 更新对象时复制属性
UserDO existingUser = userMapper.selectById(id);
BeanUtils.copyProperties(userUpdateVO, existingUser);
```

## 注意事项

1. BeanUtils 仅支持属性名和类型匹配的简单复制，不支持复杂的属性映射
2. 对于嵌套对象或需要特殊处理的字段，建议使用 MapStruct 或手动赋值
3. 所有转换方法在源对象为 null 时会返回 null，调用者需要注意空指针检查
4. 列表和分页结果的转换方法会保持原有的顺序不变
5. copyProperties 方法会忽略源对象中为 null 的属性，不会覆盖目标对象中已有的非 null 值

## 相关模块

- [collection_utils.md](collection_utils.md)：提供集合操作工具，被 BeanUtils 依赖
- [object_utils.md](object_utils.md)：同包下的其他对象工具类
- [page_utils.md](page_utils.md)：同包下的分页工具类