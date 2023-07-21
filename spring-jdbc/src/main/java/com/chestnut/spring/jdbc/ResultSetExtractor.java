package com.chestnut.spring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 结果集提取器接口，用于从ResultSet中提取数据并返回目标类型T
 *
 * @param <T> 结果集提取结果类型的泛型参数
 */
@FunctionalInterface
public interface ResultSetExtractor<T> {
    /**
     * 从ResultSet中提取数据并返回目标类型T
     *
     * @param rs ResultSet对象，包含从数据库中查询的结果数据
     * @return 提取后的目标数据，类型为T
     * @throws SQLException 如果在提取数据的过程中出现SQL异常
     */
    @Nullable
    T extractData(ResultSet rs) throws SQLException;
}
