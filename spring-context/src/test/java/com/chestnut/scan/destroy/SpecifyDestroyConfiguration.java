package com.chestnut.scan.destroy;

import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.annotation.Value;

@Configuration
public class SpecifyDestroyConfiguration {

    @Bean(destroyMethod = "destroy")
    SpecifyDestroyBean createSpecifyDestroyBean(@Value("${app.title}") String appTitle) {
        return new SpecifyDestroyBean(appTitle);
    }
}
