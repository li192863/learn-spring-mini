package com.chestnut.spring.exception;

/**
 * Bean定义不唯一异常
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class NoUniqueBeanDefinitionException extends BeanDefinitionException {
    /**
     * 创建一个空的NoUniqueBeanDefinitionException实例
     */
    public NoUniqueBeanDefinitionException() {
    }

    /**
     * 使用指定的错误消息创建一个NoUniqueBeanDefinitionException实例
     *
     * @param message 错误消息
     */
    public NoUniqueBeanDefinitionException(String message) {
        super(message);
    }
}
