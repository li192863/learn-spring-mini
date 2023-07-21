package com.chestnut.spring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接回调接口，用于在数据库连接上执行操作
 *
 * @param <T> 操作的结果类型的泛型参数
 */
@FunctionalInterface
public interface ConnectionCallback<T> {
    /**
     * 在数据库连接上执行数据库操作的方法
     *
     * @param connection 数据库连接对象
     * @return 数据库操作的结果，通常为泛型参数T指定的类
     * @throws SQLException 如果在数据库操作过程中出现SQL异常
     */
    @Nullable
    T doInConnection(Connection connection) throws SQLException;
}
