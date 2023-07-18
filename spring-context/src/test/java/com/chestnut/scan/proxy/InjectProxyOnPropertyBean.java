package com.chestnut.scan.proxy;

import com.chestnut.spring.annotation.Autowired;
import com.chestnut.spring.annotation.Component;

@Component
public class InjectProxyOnPropertyBean {

    @Autowired
    public OriginBean injected;
}
