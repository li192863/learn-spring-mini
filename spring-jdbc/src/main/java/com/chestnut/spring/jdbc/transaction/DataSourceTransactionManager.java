package com.chestnut.spring.jdbc.transaction;

import com.chestnut.spring.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据源事务管理器
 *
 * @author: Chestnut
 * @since: 2023-07-20
 **/
public class DataSourceTransactionManager implements PlatformTransactionManager, InvocationHandler {
    /**
     * 事务状态，访问该变量的每个线程都会拥有该变量的本地副本
     * ThreadLocal允许在每个线程中创建自己的本地变量，并且该变量对于其他线程是不可见的。
     * 每个线程都可以独立地修改自己的副本，而不会影响其他线程的副本。
     * 这使得在多线程环境下，可以轻松地实现线程间数据隔离，避免数据共享导致的线程安全问题。
     * 在事务中，事务状态信息需要在多个方法之间共享，但每个线程需要有自己的事务状态副本，以保证事务隔离性。
     */
    public static final ThreadLocal<TransactionStatus> transactionStatus = new ThreadLocal<>();

    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 连接池数据源
     */
    private final DataSource dataSource;

    /**
     * 创建一个DataSourceTransactionManager实例
     *
     * @param dataSource 连接池数据源
     */
    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 代理对象的方法被调用时执行
     *
     * @param proxy  代理对象
     * @param method 被调用的方法对象
     * @param args   调用方法时传递的参数数组
     * @return 调用方法的返回值
     * @throws Throwable invoke过程中抛出异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 获取当前线程的事务状态
        TransactionStatus ts = transactionStatus.get();
        // 当前线程没有事务，开始一个新的事务
        if (ts == null) {
            // 开始新事务
            try (Connection connection = this.dataSource.getConnection()) {
                final boolean autoCommit = connection.getAutoCommit();
                // 设置autoCommit为false，以便手动管理事务
                if (autoCommit) {
                    connection.setAutoCommit(false);
                }
                try {
                    // 设置当前线程的事务状态
                    transactionStatus.set(new TransactionStatus(connection));
                    // 执行实际的数据库操作
                    Object r = method.invoke(proxy, args);
                    // 事务执行成功，提交事务
                    connection.commit();
                    return r;
                } catch (InvocationTargetException e) {
                    logger.warn("will rollback transaction for caused exception: {}", e.getCause() == null ? "null" : e.getCause().getClass().getName());
                    // 事务处理异常为主要异常
                    TransactionException te = new TransactionException(e.getCause());
                    try {
                        // 回滚事务
                        connection.rollback();
                    } catch (SQLException sqle) {
                        // SQL异常为次要异常，将 sqle 作为被抑制异常添加到 te 中
                        te.addSuppressed(sqle);
                    }
                    throw te;
                } finally {
                    // 清除事务状态的本地副本
                    transactionStatus.remove();
                    // 恢复autoCommit为原值
                    if (autoCommit) {
                        connection.setAutoCommit(true);
                    }
                }
            }
        }
        // 当前线程已经有事务，则直接执行目标方法
        return method.invoke(proxy, args);
    }
}
