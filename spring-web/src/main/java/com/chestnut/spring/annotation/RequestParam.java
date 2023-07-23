package com.chestnut.spring.annotation;

import com.chestnut.spring.web.utils.WebUtils;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    /**
     * Parameter name.
     */
    String value();

    /**
     * Parameter default value.
     */
    String defaultValue() default WebUtils.DEFAULT_PARAM_VALUE;
}
