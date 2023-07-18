package com.chestnut.spring.exception;

/**
 * Bean相关异常
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class BeansException extends NestedRuntimeException {
    /**
     * 创建一个空的BeansException实例
     */
    public BeansException() {
    }

    /**
     * 使用指定的错误消息创建一个BeansException实例
     *
     * @param message 错误消息
     */
    public BeansException(String message) {
        super(message);
    }

    /**
     * 使用指定的原因创建一个BeansException实例
     *
     * @param cause 异常的原因
     */
    public BeansException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用指定的错误消息和原因创建一个BeansException实例
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
}
