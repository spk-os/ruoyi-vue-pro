# 验证模块 (validation) 文档

## 模块概述

验证模块（`validation`）是Yudao框架中的一个核心工具模块，主要用于提供数据验证功能。该模块包含了多种常用的数据验证器，用于确保数据的合法性和一致性。

### 主要功能

- **手机号验证**：验证手机号格式是否正确。
- **电话号码验证**：验证电话号码格式是否正确。
- **枚举值验证**：验证字段值是否在指定的枚举范围内。
- **字典值验证**：验证字段值是否在指定的字典数据范围内。

### 模块结构

```
validation/
├── yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/util/validation/
│   ├── ValidationUtils.java          # 验证工具类
│   ├── Mobile.java                   # 手机号验证注解
│   ├── MobileValidator.java          # 手机号验证器
│   ├── Telephone.java                # 电话号码验证注解
│   ├── TelephoneValidator.java       # 电话号码验证器
│   ├── InEnum.java                   # 枚举值验证注解
│   ├── InEnumValidator.java          # 枚举值验证器
│   ├── InEnumCollectionValidator.java # 枚举集合验证器
│   └── package-info.java             # 包信息
└── yudao-framework/yudao-spring-boot-starter-excel/src/main/java/cn/iocoder/yudao/framework/dict/validation/
    ├── InDict.java                   # 字典值验证注解
    ├── InDictValidator.java          # 字典值验证器
    └── InDictCollectionValidator.java # 字典集合验证器
```

## 核心组件详解

### 1. ValidationUtils.java

**文件路径**: `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/util/validation/ValidationUtils.java`

**功能**: 提供通用的验证方法，包括手机号、URL和XML NCName的验证。

**核心方法**:
- `isMobile(String mobile)`: 验证手机号格式。
- `isURL(String url)`: 验证URL格式。
- `isXmlNCName(String str)`: 验证XML NCName格式。
- `validate(Object object, Class<?>... groups)`: 使用默认验证器验证对象。
- `validate(Validator validator, Object object, Class<?>... groups)`: 使用指定验证器验证对象。

**代码示例**:
```java
package cn.iocoder.yudao.framework.common.util.validation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import org.springframework.util.StringUtils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 校验工具类
 *
 * @author 芋道源码
 */
public class ValidationUtils {

    private static final Pattern PATTERN_MOBILE = Pattern.compile("^(?:(?:\\+|00)86)?1(?:(?:3[\\d])|(?:4[0,1,4-9])|(?:5[0-3,5-9])|(?:6[2,5-7])|(?:7[0-8])|(?:8[\\d])|(?:9[0-3,5-9]))\\d{8}$");

    private static final Pattern PATTERN_URL = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|");

    private static final Pattern PATTERN_XML_NCNAME = Pattern.compile("[a-zA-Z_][\\-_.0-9_a-zA-Z$]*");

    public static boolean isMobile(String mobile) {
        return StringUtils.hasText(mobile)
                && PATTERN_MOBILE.matcher(mobile).matches();
    }

    public static boolean isURL(String url) {
        return StringUtils.hasText(url)
                && PATTERN_URL.matcher(url).matches();
    }

    public static boolean isXmlNCName(String str) {
        return StringUtils.hasText(str)
                && PATTERN_XML_NCNAME.matcher(str).matches();
    }

    public static void validate(Object object, Class<?>... groups) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Assert.notNull(validator);
        validate(validator, object, groups);
    }

    public static void validate(Validator validator, Object object, Class<?>... groups) {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        if (CollUtil.isNotEmpty(constraintViolations)) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

}
```

### 2. Mobile.java 和 MobileValidator.java

**文件路径**:
- `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/Mobile.java`
- `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/MobileValidator.java`

**功能**: 验证手机号格式。

**Mobile.java**:
```java
package cn.iocoder.yudao.framework.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*

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
        validatedBy = MobileValidator.class
)
public @interface Mobile {

    String message() default "手机号格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
```

**MobileValidator.java**:
```java
package cn.iocoder.yudao.framework.common.validation;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MobileValidator implements ConstraintValidator<Mobile, String> {

    @Override
    public void initialize(Mobile annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果手机号为空，默认不校验，即校验通过
        if (StrUtil.isEmpty(value)) {
            return true;
        }
        // 校验手机
        return ValidationUtils.isMobile(value);
    }

}
```

### 3. Telephone.java 和 TelephoneValidator.java

**文件路径**:
- `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/Telephone.java`
- `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/TelephoneValidator.java`

**功能**: 验证电话号码格式。

**Telephone.java**:
```java
package cn.iocoder.yudao.framework.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*

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
        validatedBy = TelephoneValidator.class
)
public @interface Telephone {

    String message() default "电话格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
```

**TelephoneValidator.java**:
```java
package cn.iocoder.yudao.framework.common.validation;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.PhoneUtil;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TelephoneValidator implements ConstraintValidator<Telephone, String> {

    @Override
    public void initialize(Telephone annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果手机号为空，默认不校验，即校验通过
        if (CharSequenceUtil.isEmpty(value)) {
            return true;
        }
        // 校验手机
        return PhoneUtil.isTel(value) || PhoneUtil.isPhone(value);
    }

}
```

### 4. InEnum.java、InEnumValidator.java 和 InEnumCollectionValidator.java

**文件路径**:
- `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/InEnum.java`
- `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/InEnumValidator.java`
- `yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/validation/InEnumCollectionValidator.java`

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

**InEnumCollectionValidator.java**:
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

### 5. InDict.java、InDictValidator.java 和 InDictCollectionValidator.java

**文件路径**:
- `yudao-framework/yudao-spring-boot-starter-excel/src/main/java/cn/iocoder/yudao/framework/dict/validation/InDict.java`
- `yudao-framework/yudao-spring-boot-starter-excel/src/main/java/cn/iocoder/yudao/framework/dict/validation/InDictValidator.java`
- `yudao-framework/yudao-spring-boot-starter-excel/src/main/java/cn/iocoder/yudao/framework/dict/validation/InDictCollectionValidator.java`

**功能**: 验证字段值是否在指定的字典数据范围内。

**InDict.java**:
```java
package cn.iocoder.yudao.framework.dict.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
        validatedBy = {InDictValidator.class, InDictCollectionValidator.class}
)
public @interface InDict {

    /**
     * 数据字典 type
     */
    String type();

    String message() default "必须在指定范围 {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
```

**InDictValidator.java**:
```java
package cn.iocoder.yudao.framework.dict.validation;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class InDictValidator implements ConstraintValidator<InDict, Object> {

    private String dictType;

    @Override
    public void initialize(InDict annotation) {
        this.dictType = annotation.type();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 为空时，默认不校验，即认为通过
        if (value == null) {
            return true;
        }
        // 校验通过
        final List<String> values = DictFrameworkUtils.getDictDataValueList(dictType);
        boolean match = values.stream().anyMatch(v -> StrUtil.equalsIgnoreCase(v, value.toString()));
        if (match) {
            return true;
        }

        // 校验不通过，自定义提示语句
        context.disableDefaultConstraintViolation(); // 禁用默认的 message 的值
        context.buildConstraintViolationWithTemplate(
                context.getDefaultConstraintMessageTemplate().replaceAll("\\{value}", values.toString())
        ).addConstraintViolation(); // 重新添加错误提示语句
        return false;
    }

}
```

**InDictCollectionValidator.java**:
```java
package cn.iocoder.yudao.framework.dict.validation;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.List;

public class InDictCollectionValidator implements ConstraintValidator<InDict, Collection<?>> {

    private String dictType;

    @Override
    public void initialize(InDict annotation) {
        this.dictType = annotation.type();
    }

    @Override
    public boolean isValid(Collection<?> list, ConstraintValidatorContext context) {
        // 为空时，默认不校验，即认为通过
        if (CollUtil.isEmpty(list)) {
            return true;
        }
        // 校验全部通过
        List<String> dbValues = DictFrameworkUtils.getDictDataValueList(dictType);
        boolean match = list.stream().allMatch(v -> dbValues.stream()
                .anyMatch(dbValue -> dbValue.equalsIgnoreCase(v.toString())));
        if (match) {
            return true;
        }

        // 校验不通过，自定义提示语句
        context.disableDefaultConstraintViolation(); // 禁用默认的 message 的值
        context.buildConstraintViolationWithTemplate(
                context.getDefaultConstraintMessageTemplate().replaceAll("\\{value}", dbValues.toString())
        ).addConstraintViolation(); // 重新添加错误提示语句
        return false;
    }

}
```

## 架构图

## 架构图描述

验证模块的架构图描述了各个组件之间的关系，主要包括以下几个部分：

1. **ValidationUtils** - 核心工具类，提供基础的验证方法，并与各个验证器相连。
2. **MobileValidator** - 手机号验证器，依赖于Mobile注解。
3. **TelephoneValidator** - 电话号码验证器，依赖于Telephone注解。
4. **InEnumValidator** - 枚举值验证器，依赖于InEnum注解，同时也与InDictValidator相关。
5. **InEnumCollectionValidator** - 枚举集合验证器，依赖于InEnum注解。
6. **InDictValidator** - 字典值验证器，依赖于InDict注解。
7. **InDictCollectionValidator** - 字典集合验证器，依赖于InDict注解。

各个组件通过注解和验证逻辑相互关联，共同构成了完整的验证模块。

## 使用示例

### 1. 手机号验证

```java
import cn.iocoder.yudao.framework.common.validation.Mobile;

public class UserDTO {
    @Mobile(message = "手机号格式不正确")
    private String mobile;
    
    // Getter 和 Setter
}
```

### 2. 电话号码验证

```java
import cn.iocoder.yudao.framework.common.validation.Telephone;

public class UserDTO {
    @Telephone(message = "电话号码格式不正确")
    private String telephone;
    
    // Getter 和 Setter
}
```

### 3. 枚举值验证

```java
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;

public class UserDTO {
    @InEnum(value = UserType.class, message = "用户类型必须在指定范围")
    private Integer type;
    
    // Getter 和 Setter
}

public enum UserType implements ArrayValuable<Integer> {
    ADMIN(1, "管理员"),
    USER(2, "普通用户");
    
    private final Integer value;
    private final String label;
    
    UserType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
    
    @Override
    public Integer[] array() {
        return new Integer[]{ADMIN.value, USER.value};
    }
}
```

### 4. 字典值验证

```java
import cn.iocoder.yudao.framework.dict.validation.InDict;

public class UserDTO {
    @InDict(type = "user_status", message = "用户状态必须在指定范围")
    private String status;
    
    // Getter 和 Setter
}
```

## 总结

验证模块（`validation`）为Yudao框架提供了强大的数据验证功能，通过使用注解和验证器，可以轻松实现对各种数据格式的验证。该模块的设计遵循了模块化和可扩展的原则，使得开发者可以方便地添加新的验证器和注解，以满足不同的业务需求。

通过本文档，开发者可以了解验证模块的核心组件、架构设计以及使用方法，从而更好地利用该模块进行数据验证，确保系统的数据质量和安全性。

