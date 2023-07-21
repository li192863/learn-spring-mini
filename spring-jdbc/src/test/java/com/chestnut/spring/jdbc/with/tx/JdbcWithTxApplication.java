package com.chestnut.spring.jdbc.with.tx;

import com.chestnut.spring.annotation.ComponentScan;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.annotation.Import;
import com.chestnut.spring.jdbc.JdbcConfiguration;

@ComponentScan
@Configuration
@Import(JdbcConfiguration.class)
public class JdbcWithTxApplication {

}
