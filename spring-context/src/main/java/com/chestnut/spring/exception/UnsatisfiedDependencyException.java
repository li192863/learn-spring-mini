package com.chestnut.spring.exception;

/**
 * Bean依赖项不满足异常
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class UnsatisfiedDependencyException extends BeanCreationException {
    /**
     * 创建一个空的UnsatisfiedDependencyException实例
     */
    public UnsatisfiedDependencyException() {
    }

    /**
     * 使用指定的错误消息创建一个UnsatisfiedDependencyException实例
     *
     * @param message 错误消息
     */
    public UnsatisfiedDependencyException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息和原因创建一个UnsatisfiedDependencyException实例
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public UnsatisfiedDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用指定的原因创建一个UnsatisfiedDependencyException实例
     *
     * @param cause 异常的原因
     */
    public UnsatisfiedDependencyException(Throwable cause) {
        super(cause);
    }
}
