package com.chestnut.spring.exception;

/**
 * Bean创建异常
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class BeanCreationException extends BeansException {
    /**
     * 创建一个空的BeanCreationException实例
     */
    public BeanCreationException() {
    }

    /**
     * 使用指定的错误消息创建一个BeanCreationException实例
     *
     * @param message 错误消息
     */
    public BeanCreationException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息创建一个BeanCreationException实例
     *
     * @param cause 异常的原因
     */
    public BeanCreationException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用指定的错误消息和原因创建一个BeanCreationException实例
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
