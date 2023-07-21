package com.chestnut.spring.annotation;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Order {
    /**
     * The order value.
     */
    int value();
}
