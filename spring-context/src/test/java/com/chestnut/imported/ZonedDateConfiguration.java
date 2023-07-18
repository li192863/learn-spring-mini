package com.chestnut.imported;

import java.time.ZonedDateTime;

import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.Configuration;

@Configuration
public class ZonedDateConfiguration {

    @Bean
    ZonedDateTime startZonedDateTime() {
        return ZonedDateTime.now();
    }
}
