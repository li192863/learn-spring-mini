package com.chestnut.spring.jdbc.transaction;

import java.sql.Connection;

/**
 * 事务状态类，内部只有连接池数据源
 *
 * @author: Chestnut
 * @since: 2023-07-20
 **/
public class TransactionStatus {
    /**
     * 数据库连接
     */
    private final Connection connection;

    /**
     * 创建一个TransactionStatus实例
     *
     * @param connection 数据库连接
     */
    public TransactionStatus(Connection connection) {
        this.connection = connection;
    }

    /**
     * 获取数据库连接
     *
     * @return 数据库连接
     */
    public Connection getConnection() {
        return connection;
    }
}
