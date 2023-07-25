package com.chestnut.spring.hello;

import com.chestnut.spring.annotation.ComponentScan;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.annotation.Import;
import com.chestnut.spring.jdbc.JdbcConfiguration;
import com.chestnut.spring.web.WebMvcConfiguration;

@ComponentScan
@Configuration
@Import({ JdbcConfiguration.class, WebMvcConfiguration.class })
public class HelloConfiguration {

}
