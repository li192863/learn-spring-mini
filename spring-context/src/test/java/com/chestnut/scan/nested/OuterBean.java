package com.chestnut.scan.nested;

import com.chestnut.spring.annotation.Component;

@Component
public class OuterBean {

    @Component
    public static class NestedBean {

    }
}
