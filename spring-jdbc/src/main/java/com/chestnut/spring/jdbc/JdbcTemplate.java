package com.chestnut.spring.jdbc;

import com.chestnut.spring.exception.DataAccessException;
import com.chestnut.spring.jdbc.tx.TransactionalUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC模板类
 *
 * @author: Chestnut
 * @since: 2023-07-20
 **/
public class JdbcTemplate {
    /**
     * 连接池数据源
     */
    private final DataSource dataSource;

    /**
     * 创建一个JdbcTemplate对象
     *
     * @param dataSource 数据源，用于获取数据库连接以执行SQL语句
     */
    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 执行 SQL 查询并返回一个数值类型的结果
     *
     * @param sql  SQL查询语句
     * @param args SQL查询的参数列表
     * @return 查询结果的数值类型的结果，类型为Number
     * @throws DataAccessException 如果查询结果为空
     */
    public Number queryForNumber(String sql, Object... args) throws DataAccessException {
        return queryForObject(sql, NumberRowMapper.instance, args);
    }

    /**
     * 执行 SQL 查询并返回单个结果
     *
     * @param sql       SQL 查询语句
     * @param rowMapper 用于将结果集映射到目标类型 T 的 RowMapper 实例
     * @param args      SQL 查询的参数列表
     * @param <T>       查询结果的目标类型 T 的单个结果
     * @return 结果的目标类型的泛型参数
     * @throws DataAccessException 如果查询结果为空，或者多个结果行被找到
     */
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return execute(preparedStatementCreator(sql, args),
                // PreparedStatementCallback
                (PreparedStatement ps) -> {
                    T t = null;
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (t == null) {
                                t = rowMapper.mapRow(rs, rs.getRow());
                            } else {
                                throw new DataAccessException("Multiple rows found.");
                            }
                        }
                    }
                    if (t == null) {
                        throw new DataAccessException("Empty result set.");
                    }
                    return t;
                });
    }

    /**
     * 根据目标类型clazz进行不同类型的数据库查询，并返回目标类型的单个结果
     *
     * @param sql   SQL查询语句
     * @param clazz 结果的目标类型的Class对象
     * @param args  SQL查询的参数列表
     * @param <T>   查询结果的目标类型T的单个结果
     * @return 结果的目标类型的泛型参数
     * @throws DataAccessException 如果查询结果为空，或者多个结果行被找到，则抛出DataAccessException异常
     */
    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> clazz, Object... args) throws DataAccessException {
        if (clazz == String.class) {
            return (T) queryForObject(sql, StringRowMapper.instance, args);
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) queryForObject(sql, BooleanRowMapper.instance, args);
        }
        if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive()) {
            return (T) queryForObject(sql, NumberRowMapper.instance, args);
        }
        return queryForObject(sql, new BeanRowMapper<>(clazz), args);
    }

    /**
     * 执行 SQL 查询并返回一个目标类型T的结果列表
     *
     * @param sql       SQL查询语句
     * @param rowMapper 用于将结果集映射到目标类型T的RowMapper实例
     * @param args      SQL查询的参数列表
     * @param <T>       查询结果的目标类型T的结果列表
     * @return 结果的目标类型的泛型参数
     * @throws DataAccessException 如果在查询过程中出现异常
     */
    public <T> List<T> queryForList(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return execute(preparedStatementCreator(sql, args),
                // PreparedStatementCallback
                (PreparedStatement ps) -> {
                    List<T> list = new ArrayList<>();
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(rowMapper.mapRow(rs, rs.getRow()));
                        }
                    }
                    return list;
                });
    }


    /**
     * 执行 SQL 查询并返回一个目标类型T的结果列表
     *
     * @param sql   SQL查询语句
     * @param clazz 结果的目标类型的Class对象
     * @param args  SQL查询的参数列表
     * @param <T>   结果的目标类型的泛型参数
     * @return 查询结果的目标类型T的结果列表
     * @throws DataAccessException 结果的目标类型的泛型参数
     */
    public <T> List<T> queryForList(String sql, Class<T> clazz, Object... args) throws DataAccessException {
        return queryForList(sql, new BeanRowMapper<>(clazz), args);
    }

    /**
     * 执行 SQL 更新操作并返回生成的主键值（自动生成的主键）
     *
     * @param sql  SQL更新操作语句
     * @param args SQL更新操作的参数列表
     * @return 生成的主键值
     * @throws DataAccessException 如果更新操作没有插入任何行，或者插入了多行，或者在获取生成的主键时出现异常
     */
    public Number updateAndReturnGeneratedKey(String sql, Object... args) throws DataAccessException {
        return execute(
                // PreparedStatementCreator
                (Connection con) -> {
                    PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    bindArgs(ps, args);
                    return ps;
                },
                // PreparedStatementCallback
                (PreparedStatement ps) -> {
                    int n = ps.executeUpdate();
                    if (n == 0) {
                        throw new DataAccessException("0 rows inserted.");
                    }
                    if (n > 1) {
                        throw new DataAccessException("Multiple rows inserted.");
                    }
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            return (Number) keys.getObject(1);
                        }
                    }
                    throw new DataAccessException("Should not reach here.");
                });
    }

    /**
     * 执行 SQL 更新操作并返回更新的行数
     *
     * @param sql  SQL更新操作语句
     * @param args SQL更新操作的参数列表
     * @return 更新的行数
     * @throws DataAccessException 如果更新操作失败
     */
    public int update(String sql, Object... args) throws DataAccessException {
        return execute(preparedStatementCreator(sql, args),
                // PreparedStatementCallback
                (PreparedStatement ps) -> ps.executeUpdate());
    }

    /**
     * 在数据库连接上执行数据库操作，并在数据库连接上进行事务管理
     *
     * @param action 数据库连接回调接口，用于在数据库连接上执行操作
     * @param <T>    数据库操作返回值的类型
     * @return 数据库操作的返回值
     * @throws DataAccessException 如果在数据库操作过程中出现异常
     */
    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
        // 尝试获取当前事务连接
        Connection currentConnection = TransactionalUtils.getCurrentConnection();
        if (currentConnection != null) {
            try {
                return action.doInConnection(currentConnection);
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
        }
        // 获取新连接
        try (Connection newConnection = this.dataSource.getConnection()) {
            final boolean autoCommit = newConnection.getAutoCommit();
            // 设置autoCommit为true
            if (!autoCommit) {
                newConnection.setAutoCommit(true);
            }
            T result = action.doInConnection(newConnection);
            // 恢复autoCommit为原值
            if (!autoCommit) {
                newConnection.setAutoCommit(false);
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /**
     * 在预编译语句上执行数据库操作
     *
     * @param psc    预编译语句创建接口，用于创建预编译语句
     * @param action 预编译语句回调接口，用于在预编译语句上执行数据库操作
     * @param <T>    数据库操作返回值的类型
     * @return 数据库操作的返回值
     */
    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) {
        return execute((Connection connection) -> {
            // 使用预编译语句对象创建器创建的语句参数已经绑定好
            try (PreparedStatement ps = psc.createPreparedStatement(connection)) {
                return action.doInPreparedStatement(ps);
            }
        });
    }

    /**
     * 创建预编译语句对象创建器，并绑定参数
     *
     * @param sql  预编译语句的SQL语句模板
     * @param args 预编译语句中的参数列表
     * @return 预编译语句创建对象创建器
     */
    private PreparedStatementCreator preparedStatementCreator(String sql, Object... args) {
        return (Connection connection) -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            bindArgs(ps, args);
            return ps;
        };
    }

    /**
     * 将参数绑定到预编译语句对象中的辅助方法
     *
     * @param ps   预编译语句对象，用于执行数据库操作
     * @param args 预编译语句中的参数列表
     * @throws SQLException 如果在参数绑定的过程中出现SQL异常
     */
    private void bindArgs(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }
}

/**
 * 字符串行映射类
 */
class StringRowMapper implements RowMapper<String> {

    static StringRowMapper instance = new StringRowMapper();

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString(1);
    }
}

/**
 * 布尔值行映射类
 */
class BooleanRowMapper implements RowMapper<Boolean> {

    static BooleanRowMapper instance = new BooleanRowMapper();

    @Override
    public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getBoolean(1);
    }
}

/**
 * 数字值行映射类
 */
class NumberRowMapper implements RowMapper<Number> {

    static NumberRowMapper instance = new NumberRowMapper();

    @Override
    public Number mapRow(ResultSet rs, int rowNum) throws SQLException {
        return (Number) rs.getObject(1);
    }
}
