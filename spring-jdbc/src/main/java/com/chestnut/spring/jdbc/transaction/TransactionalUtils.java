package com.chestnut.spring.jdbc.transaction;

import jakarta.annotation.Nullable;

import java.sql.Connection;

/**
 * 事务工具类
 *
 * @author: Chestnut
 * @since: 2023-07-20
 **/
public class TransactionalUtils {
    /**
     * 获取当前线程的数据库连接
     *
     * @return 数据库连接
     */
    @Nullable
    public static Connection getCurrentConnection() {
        TransactionStatus ts = DataSourceTransactionManager.transactionStatus.get();
        return ts == null ? null : ts.getConnection();
    }
}
