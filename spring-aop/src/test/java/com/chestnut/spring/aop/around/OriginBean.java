package com.chestnut.spring.aop.around;

import com.chestnut.spring.annotation.Around;
import com.chestnut.spring.annotation.Component;
import com.chestnut.spring.annotation.Value;

@Component
@Around("aroundInvocationHandler")
public class OriginBean {

    @Value("${customer.name}")
    public String name;

    @Polite
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}
