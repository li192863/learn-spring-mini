package com.chestnut.spring.jdbc;

import com.chestnut.spring.annotation.Autowired;
import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.annotation.Value;
import com.chestnut.spring.jdbc.transaction.DataSourceTransactionManager;
import com.chestnut.spring.jdbc.transaction.PlatformTransactionManager;
import com.chestnut.spring.jdbc.transaction.TransactionalBeanPostProcessor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * JDBC配置类
 *
 * @author: Chestnut
 * @since: 2023-07-20
 **/
@Configuration
public class JdbcConfiguration {
    /**
     * 连接池数据源工厂方法
     *
     * @param url             数据库连接URL
     * @param username        数据库用户名
     * @param password        数据库密码
     * @param driver          数据库驱动类名（可选，如果不指定，则使用默认值）
     * @param maximumPoolSize 最大连接池大小（可选，如果不指定，则使用默认值为20）
     * @param minimumPoolSize 最小连接池大小（可选，如果不指定，则使用默认值为1）
     * @param connTimeout     连接超时时间（可选，如果不指定，则使用默认值为30000毫秒）
     * @return 配置完成的连接池数据源
     */
    @Bean(destroyMethod = "close")
    public DataSource dataSource(
            // properties:
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name:}") String driver,
            @Value("${spring.datasource.maximum-pool-size:20}") int maximumPoolSize,
            @Value("${spring.datasource.minimum-pool-size:1}") int minimumPoolSize,
            @Value("${spring.datasource.connection-timeout:30000}") int connTimeout
    ) {
        // 创建Hikari连接池配置对象
        HikariConfig config = new HikariConfig();
        // 设置自动提交事务为false，即使用手动提交事务
        config.setAutoCommit(false);
        // 设置数据库连接URL、用户名和密码
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        // 如果指定了数据库驱动类名，则设置驱动类名（若未指定，连接池会根据URL自动识别驱动）
        if (driver != null) {
            config.setDriverClassName(driver);
        }
        // 设置连接池的最大连接数和最小空闲连接数
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumPoolSize);
        // 设置连接超时时间
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

    /**
     * JDBC模板类工厂方法
     *
     * @param dataSource 连接池数据源
     * @return JDBC模板类
     */
    @Bean
    public JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 事务Bean后处理器工厂方法
     *
     * @return 事务Bean后处理器
     */
    @Bean
    public TransactionalBeanPostProcessor transactionalBeanPostProcessor() {
        return new TransactionalBeanPostProcessor();
    }

    /**
     * 事务管理器接口工厂方法
     *
     * @param dataSource 连接池数据源
     * @return 事务管理器接口
     */
    @Bean
    public PlatformTransactionManager platformTransactionManager(@Autowired DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
