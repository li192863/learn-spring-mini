package com.chestnut.spring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 预编译语句回调接口，用于在预编译语句（PreparedStatement）上执行数据库操作
 *
 * @param <T> 操作的结果类型的泛型参数
 */
@FunctionalInterface
public interface PreparedStatementCallback<T> {
    /**
     * 在预编译语句上执行数据库操作的方法
     *
     * @param preparedStatement 预编译语句对象
     * @return 数据库操作的结果，通常为泛型参数T指定的类
     * @throws SQLException 如果在数据库操作过程中出现SQL异常
     */
    @Nullable
    T doInPreparedStatement(PreparedStatement preparedStatement) throws SQLException;
}
