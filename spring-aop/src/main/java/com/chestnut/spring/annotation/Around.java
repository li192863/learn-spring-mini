package com.chestnut.spring.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited // 注解的定义可继承，父类包含此注解时，其子类也会包含相同注解
@Documented
public @interface Around {

    /**
     * Invocation handler bean name.
     */
    String value();
}