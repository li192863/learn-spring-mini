package com.chestnut.scan.proxy;

import com.chestnut.spring.annotation.Autowired;
import com.chestnut.spring.annotation.Component;

@Component
public class InjectProxyOnConstructorBean {

    public final OriginBean injected;

    public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }
}
