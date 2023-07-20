package com.chestnut.spring.aop.after;

import com.chestnut.spring.annotation.Around;
import com.chestnut.spring.annotation.Component;

@Component
@Around("politeInvocationHandler")
public class GreetingBean {

    public String hello(String name) {
        return "Hello, " + name + ".";
    }

    public String morning(String name) {
        return "Morning, " + name + ".";
    }
}
