package com.chestnut.scan.primary;

import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.annotation.Primary;

@Configuration
public class PrimaryConfiguration {

    @Primary
    @Bean
    DogBean husky() {
        return new DogBean("Husky");
    }

    @Bean
    DogBean teddy() {
        return new DogBean("Teddy");
    }
}
