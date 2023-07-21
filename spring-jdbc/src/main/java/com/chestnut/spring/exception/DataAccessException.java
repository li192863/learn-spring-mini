package com.chestnut.spring.exception;

/**
 * 数据获取异常
 *
 * @author: Chestnut
 * @since: 2023-07-20
 **/
public class DataAccessException extends NestedRuntimeException {
    /**
     * 创建一个空的DataAccessException实例
     */
    public DataAccessException() {
    }

    /**
     * 使用指定的错误消息创建一个DataAccessException实例
     *
     * @param message 错误消息
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * 使用指定的原因创建一个DataAccessException实例
     *
     * @param cause 异常的原因
     */
    public DataAccessException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用指定的错误消息和原因创建一个DataAccessException实例
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
