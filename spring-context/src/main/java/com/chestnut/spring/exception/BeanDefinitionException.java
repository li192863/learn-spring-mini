package com.chestnut.spring.exception;

/**
 * Bean定义异常
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class BeanDefinitionException extends BeansException {
    /**
     * 创建一个空的BeanDefinitionException实例
     */
    public BeanDefinitionException() {
    }

    /**
     * 使用指定的错误消息创建一个BeanDefinitionException实例
     *
     * @param message 错误消息
     */
    public BeanDefinitionException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息和原因创建一个BeanDefinitionException实例
     *
     * @param cause 异常的原因
     */
    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用指定的错误消息和原因创建一个BeanDefinitionException实例
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
