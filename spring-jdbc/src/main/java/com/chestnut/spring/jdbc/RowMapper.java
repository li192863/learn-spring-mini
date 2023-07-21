package com.chestnut.spring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 行映射接口，用于将数据库查询结果集中的一行数据映射为对象
 *
 * @param <T> 映射的目标对象类型的泛型参数
 */
@FunctionalInterface
public interface RowMapper<T> {
    /**
     * 将数据库查询结果集中的一行数据映射为对象
     *
     * @param rs     数据库查询结果集对象，包含查询的结果数据
     * @param rowNum 行号，表示当前处理的结果集行数（从1开始）
     * @return 映射后的目标对象
     * @throws SQLException 如果在映射的过程中出现SQL异常
     */
    @Nullable
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
