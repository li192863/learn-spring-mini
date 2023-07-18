package com.chestnut.spring.exception;

/**
 * Bean类型不匹配异常
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class BeanNotOfRequiredTypeException extends BeansException {
    /**
     * 创建一个空的BeanNotOfRequiredTypeException实例
     */
    public BeanNotOfRequiredTypeException() {
    }

    /**
     * 使用指定的错误消息创建一个BeanNotOfRequiredTypeException实例
     *
     * @param message 错误消息
     */
    public BeanNotOfRequiredTypeException(String message) {
        super(message);
    }
}
