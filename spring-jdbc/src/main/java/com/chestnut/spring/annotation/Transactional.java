package com.chestnut.spring.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Transactional {
    /**
     * Transaction Manager, also invocation handler bean name.
     */
    String value() default "platformTransactionManager";
}
