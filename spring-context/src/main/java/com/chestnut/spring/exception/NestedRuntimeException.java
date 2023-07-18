package com.chestnut.spring.exception;

/**
 * 内嵌运行时异常
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class NestedRuntimeException extends RuntimeException {
    /**
     * 创建一个空的NestedRuntimeException实例
     */
    public NestedRuntimeException() {
    }

    /**
     * 使用指定的错误消息创建一个NestedRuntimeException实例
     *
     * @param message 错误消息
     */
    public NestedRuntimeException(String message) {
        super(message);
    }

    /**
     * 使用指定的原因创建一个NestedRuntimeException实例
     *
     * @param cause 异常的原因
     */
    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用指定的错误消息和原因创建一个NestedRuntimeException实例
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
