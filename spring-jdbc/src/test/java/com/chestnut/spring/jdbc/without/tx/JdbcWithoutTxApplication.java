package com.chestnut.spring.jdbc.without.tx;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.chestnut.spring.annotation.Autowired;
import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.ComponentScan;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.annotation.Value;
import com.chestnut.spring.jdbc.JdbcTemplate;

@ComponentScan
@Configuration
public class JdbcWithoutTxApplication {

    @Bean(destroyMethod = "close")
    DataSource dataSource(
            // properties:
            @Value("${spring.datasource.url}") String url, //
            @Value("${spring.datasource.username}") String username, //
            @Value("${spring.datasource.password}") String password, //
            @Value("${spring.datasource.driver-class-name:}") String driver, //
            @Value("${spring.datasource.maximum-pool-size:20}") int maximumPoolSize, //
            @Value("${spring.datasource.minimum-pool-size:1}") int minimumPoolSize, //
            @Value("${spring.datasource.connection-timeout:30000}") int connTimeout //
    ) {
        // 创建Druid连接池对象
        DruidDataSource dataSource = new DruidDataSource();
        // 设置数据库连接URL、用户名和密码
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        // 如果指定了数据库驱动类名，则设置驱动类名（Druid会自动识别驱动，无需显式设置）
        if (driver != null) {
            dataSource.setDriverClassName(driver);
        }
        // 设置连接池的最大连接数和最小空闲连接数
        dataSource.setMaxActive(maximumPoolSize);
        dataSource.setMinIdle(minimumPoolSize);
        // 设置连接超时时间
        dataSource.setConnectTimeout(connTimeout);
        return dataSource;
    }

    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
