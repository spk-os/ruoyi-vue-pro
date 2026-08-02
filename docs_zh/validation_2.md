# 验证模块2 (validation_2) 文档

## 模块概述

验证模块2（`validation_2`）是Yudao框架中的一个核心工具模块，主要用于提供枚举值验证功能。该模块位于 `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/` 路径下，包含了枚举值和枚举集合的验证器实现。

### 主要功能

- **枚举值验证**：验证单个字段值是否在指定的枚举范围内
- **枚举集合验证**：验证集合中的所有元素是否都在指定的枚举范围内
- **手机号验证**：验证手机号格式是否正确（与validation模块共享）
- **电话号码验证**：验证电话号码格式是否正确（与validation模块共享）

> 注意：手机号验证和电话号码验证功能与validation模块共享，详细信息请参考[validation.md](./validation.md)文档。

### 模块结构

```
validation_2/
├── InEnum.java                   # 枚举值验证注解
├── InEnumValidator.java          # 枚举值验证器
├── InEnumCollectionValidator.java # 枚举集合验证器
├── Mobile.java                   # 手机号验证注解（与validation模块共享）
├── MobileValidator.java          # 手机号验证器（与validation模块共享）
├── Telephone.java                # 电话号码验证注解（与validation模块共享）
└── TelephoneValidator.java       # 电话号码验证器（与validation模块共享）
```

## 核心组件详解

### 1. InEnum.java 和 InEnumValidator.java

**文件路径**:
- `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/InEnum.java`
- `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/InEnumValidator.java`

**功能**: 验证字段值是否在指定的枚举范围内。

**InEnum.java**:
```java
package cn.iocoder.yudao.framework.common.validation;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = {InEnumValidator.class, InEnumCollectionValidator.class}
)
public @interface InEnum {

    /**
     * @return 实现 ArrayValuable 接口的类
     */
    Class<? extends ArrayValuable<?>> value();

    String message() default "必须在指定范围 {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
```

**InEnumValidator.java**:
```java
package cn.iocoder.yudao.framework.common.validation;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InEnumValidator implements ConstraintValidator<InEnum, Object> {

    private List<?> values;

    @Override
    public void initialize(InEnum annotation) {
        ArrayValuable<?>[] values = annotation.value().getEnumConstants();
        if (values.length == 0) {
            this.values = Collections.emptyList();
        } else {
            this.values = Arrays.asList(values[0].array());
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 为空时，默认不校验，即认为通过
        if (value == null) {
            return true;
        }
        // 校验通过
        if (values.contains(value)) {
            return true;
        }
        // 校验不通过，自定义提示语句
        context.disableDefaultConstraintViolation(); // 禁用默认的 message 的值
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate()
                .replaceAll("\\{value}", values.toString())).addConstraintViolation(); // 重新添加错误提示语句
        return false;
    }

}
```

### 2. InEnumCollectionValidator.java

**文件路径**: `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/InEnumCollectionValidator.java`

**功能**: 验证集合中的所有元素是否都在指定的枚举范围内。

```java
package cn.iocoder.yudao.framework.common.validation;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InEnumCollectionValidator implements ConstraintValidator<InEnum, Collection<?>> {

    private List<?> values;

    @Override
    public void initialize(InEnum annotation) {
        ArrayValuable<?>[] values = annotation.value().getEnumConstants();
        if (values.length == 0) {
            this.values = Collections.emptyList();
        } else {
            this.values = Arrays.asList(values[0].array());
        }
    }

    @Override
    public boolean isValid(Collection<?> list, ConstraintValidatorContext context) {
        if (list == null) {
            return true;
        }
        // 校验通过
        if (CollUtil.containsAll(values, list)) {
            return true;
        }
        // 校验不通过，自定义提示语句
        context.disableDefaultConstraintViolation(); // 禁用默认的 message 的值
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate()
                .replaceAll("\\{value}", CollUtil.join(list, ","))).addConstraintViolation(); // 重新添加错误提示语句
        return false;
    }

}
```

## 架构说明

validation_2模块包含以下核心组件：
- 枚举值验证器 (InEnumValidator)：验证单个字段值是否在指定枚举范围内
- 枚举集合验证器 (InEnumCollectionValidator)：验证集合中所有元素是否都在指定枚举范围内
- 手机号验证器 (MobileValidator)：与validation模块共享的手机号验证组件
- 电话号码验证器 (TelephoneValidator)：与validation模块共享的电话号码验证组件

其中，InEnumValidator和InEnumCollectionValidator都实现了ConstraintValidator接口，用于支持@InEnum注解。
MobileValidator和TelephoneValidator与validation模块中的实现完全相同，是共享的验证组件。

## 使用示例

### 1. 枚举值验证

首先定义一个实现ArrayValuable接口的枚举类：

```java
package cn.iocoder.yudao.framework.common.core;

public interface ArrayValuable<T> {
    T[] array();
}

// 示例枚举实现
package cn.iocoder.yudao.module.system.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;

public enum UserTypeEnum implements ArrayValuable<UserTypeEnum> {
    ADMIN(1, "管理员"),
    USER(2, "普通用户"),
    GUEST(3, "访客");

    private final Integer value;
    private final String name;

    UserTypeEnum(Integer value, String name) {
        this.value = value;
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    @Override
    public UserTypeEnum[] array() {
        return values();
    }
}
```

然后在DTO或实体类中使用注解：

```java
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.system.enums.UserTypeEnum;

public class UserDTO {
    @InEnum(value = UserTypeEnum.class, message = "用户类型必须是ADMIN、USER或GUEST之一")
    private Integer userType;
    
    // Getter 和 Setter
}
```

### 2. 枚举集合验证

```java
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.system.enums.UserTypeEnum;
import java.util.List;

public class UserBatchDTO {
    @InEnum(value = UserTypeEnum.class, message = "所有用户类型必须是ADMIN、USER或GUEST之一")
    private List<Integer> userTypes;
    
    // Getter 和 Setter
}
```

### 3. 手机号验证（共享组件）

详细用法请参考[validation.md](./validation.md)文档。

### 4. 电话号码验证（共享组件）

详细用法请参考[validation.md](./validation.md)文档。

## 与其他模块的关系

validation_2模块与validation模块紧密相关，共同构成了Yudao框架的验证体系：

- validation模块提供了基础的验证工具类（ValidationUtils）和手机/电话号码验证
- validation_2模块提供了枚举值和枚举集合验证功能
- 两个模块中的Mobile、MobileValidator、Telephone、TelephoneValidator是完全相同的实现，共享同一套代码

在实际使用中，这两个模块通常会一起被引用和使用，共同提供完整的数据验证能力。

## 最佳实践

1. **枚举设计**：当需要验证一组固定值时，考虑使用枚举类实现ArrayValuable接口
2. **错误信息**：自定义验证失败时的错误信息，使其更具业务意义
3. **空值处理**：所有验证器都默认允许空值通过验证，如需强制非空，请额外添加@NotNull注解
4. **性能考虑**：枚举值在初始化时只加载一次，后续验证操作性能良好
5. **分层验证**：在Controller层使用这些验证器进行参数校验，在Service层进行业务逻辑验证

## 与Spring Boot的集成

这些验证器完全符合Jakarta Bean Validation (JSR-380)规范，可以直接在Spring Boot项目中使用：

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping
    public Result<?> createUser(@Valid @RequestBody UserDTO userDTO) {
        // 如果验证失败，会自动返回400 Bad Request和详细错误信息
        return Result.success(userService.createUser(userDTO));
    }
}
```

当验证失败时，Spring Boot会自动处理ConstraintViolationException并返回适当的错误响应。