package com.chestnut.spring.aop.after;

import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.ComponentScan;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.aop.AroundProxyBeanPostProcessor;

@Configuration
@ComponentScan
public class AfterApplication {

    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}
