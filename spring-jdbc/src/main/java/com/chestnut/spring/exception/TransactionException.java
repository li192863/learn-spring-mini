package com.chestnut.spring.exception;

/**
 * 事务处理异常
 *
 * @author: Chestnut
 * @since: 2023-07-20
 **/
public class TransactionException extends DataAccessException {
    /**
     * 创建一个空的TransactionException实例
     */
    public TransactionException() {
    }

    /**
     * 使用指定的错误消息创建一个TransactionException实例
     *
     * @param message 错误消息
     */
    public TransactionException(String message) {
        super(message);
    }

    /**
     * 使用指定的原因创建一个TransactionException实例
     *
     * @param cause 异常的原因
     */
    public TransactionException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用指定的错误消息和原因创建一个TransactionException实例
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
