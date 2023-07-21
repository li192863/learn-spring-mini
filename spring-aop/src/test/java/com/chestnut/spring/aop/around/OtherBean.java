package com.chestnut.spring.aop.around;

import com.chestnut.spring.annotation.Autowired;
import com.chestnut.spring.annotation.Component;
import com.chestnut.spring.annotation.Order;

@Order(0)
@Component
public class OtherBean {

    public OriginBean origin;

    public OtherBean(@Autowired OriginBean origin) {
        this.origin = origin;
    }
}
