package com.chestnut.spring.exception;

/**
 * Bean不存在异常
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class NoSuchBeanDefinitionException extends BeanDefinitionException {
    /**
     * 创建一个空的NoSuchBeanDefinitionException实例
     */
    public NoSuchBeanDefinitionException() {
    }

    /**
     * 使用指定的错误消息创建一个NoSuchBeanDefinitionException实例
     *
     * @param message 错误消息
     */
    public NoSuchBeanDefinitionException(String message) {
        super(message);
    }
}
