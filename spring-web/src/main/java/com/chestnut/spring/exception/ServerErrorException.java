package com.chestnut.spring.exception;

/**
 * 服务端错误异常，响应状态码500
 *
 * @author: Chestnut
 * @since: 2023-07-22
 **/
public class ServerErrorException extends ErrorResponseException {
    /**
     * 创建一个ServerErrorException实例，响应状态码为500
     */
    public ServerErrorException() {
        super(500);
    }

    /**
     * 使用指定的错误消息创建一个ServerErrorException实例，响应状态码为500
     *
     * @param message 错误消息
     */
    public ServerErrorException(String message) {
        super(500, message);
    }

    /**
     * 使用指定的原因创建一个ServerErrorException实例，响应状态码为500
     *
     * @param cause 异常的原因
     */
    public ServerErrorException(Throwable cause) {
        super(500, cause);
    }

    /**
     * 使用指定的错误消息和原因创建一个ServerErrorException实例，响应状态码为500
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public ServerErrorException(String message, Throwable cause) {
        super(500, message, cause);
    }
}
