package com.chestnut.spring.aop.around;

import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.ComponentScan;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.aop.AroundProxyBeanPostProcessor;

@Configuration
@ComponentScan
public class AroundApplication {

    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}
