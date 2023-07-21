package com.chestnut.spring.exception;

/**
 * AOP配置异常
 *
 * @author: Chestnut
 * @since: 2023-07-19
 **/
public class AopConfigException extends NestedRuntimeException {
    /**
     * 创建一个空的AopConfigException实例
     */
    public AopConfigException() {
    }

    /**
     * 使用指定的错误消息创建一个AopConfigException实例
     *
     * @param message 错误消息
     */
    public AopConfigException(String message) {
        super(message);
    }

    /**
     * 使用指定的原因创建一个AopConfigException实例
     *
     * @param cause 异常的原因
     */
    public AopConfigException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用指定的错误消息和原因创建一个AopConfigException实例
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public AopConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
