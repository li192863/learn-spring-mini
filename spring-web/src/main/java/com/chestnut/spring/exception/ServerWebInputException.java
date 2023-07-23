package com.chestnut.spring.exception;

/**
 * 客户端错误异常，响应状态码400
 *
 * @author: Chestnut
 * @since: 2023-07-22
 **/
public class ServerWebInputException extends ErrorResponseException {
    /**
     * 创建一个ServerWebInputException实例，响应状态码为400
     */
    public ServerWebInputException() {
        super(400);
    }

    /**
     * 使用指定的错误消息创建一个ServerWebInputException实例，响应状态码为400
     *
     * @param message 错误消息
     */
    public ServerWebInputException(String message) {
        super(400, message);
    }

    /**
     * 使用指定的原因创建一个ServerWebInputException实例，响应状态码为400
     *
     * @param cause 异常的原因
     */
    public ServerWebInputException(Throwable cause) {
        super(400, cause);
    }

    /**
     * 使用指定的错误消息和原因创建一个ServerWebInputException实例，响应状态码为400
     *
     * @param message 错误消息
     * @param cause   异常的原因
     */
    public ServerWebInputException(String message, Throwable cause) {
        super(400, message, cause);
    }
}
