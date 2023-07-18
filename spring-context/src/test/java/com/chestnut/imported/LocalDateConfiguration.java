package com.chestnut.imported;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.Configuration;

@Configuration
public class LocalDateConfiguration {

    @Bean
    LocalDate startLocalDate() {
        return LocalDate.now();
    }

    @Bean
    LocalDateTime startLocalDateTime() {
        return LocalDateTime.now();
    }
}
