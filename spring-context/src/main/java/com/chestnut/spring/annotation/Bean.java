package com.chestnut.spring.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
    /**
     * Bean name. default to method name.
     */
    String value() default "";

    /**
     * Initialization method.
     */
    String initMethod() default "";

    /**
     * Destroy method.
     * @return
     */
    String destroyMethod() default "";
}
