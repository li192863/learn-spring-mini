package com.chestnut.spring.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 预编译语句创建接口，用于创建预编译语句
 */
@FunctionalInterface
public interface PreparedStatementCreator {
    /**
     * 创建预编译语句
     *
     * @param connection 数据库连接
     * @return 创建的预编译语句
     * @throws SQLException 如果在创建预编译语句的过程中出现SQL异常
     */
    PreparedStatement createPreparedStatement(Connection connection) throws SQLException;
}
