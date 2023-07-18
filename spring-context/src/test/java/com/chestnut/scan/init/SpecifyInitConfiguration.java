package com.chestnut.scan.init;

import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.annotation.Value;

@Configuration
public class SpecifyInitConfiguration {

    @Bean(initMethod = "init")
    SpecifyInitBean createSpecifyInitBean(@Value("${app.title}") String appTitle, @Value("${app.version}") String appVersion) {
        return new SpecifyInitBean(appTitle, appVersion);
    }
}
