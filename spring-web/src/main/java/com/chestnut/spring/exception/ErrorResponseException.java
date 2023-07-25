package com.chestnut.spring.exception;

/**
 * 响应错误异常
 *
 * @author: Chestnut
 * @since: 2023-07-22
 **/
public class ErrorResponseException extends NestedRuntimeException {
    /**
     * 响应状态码
     */
    public final int statusCode;

    /**
     * 创建一个ErrorResponseException实例
     *
     * @param statusCode 响应状态码
     */
    public ErrorResponseException(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * 使用响应状态码和指定的错误消息创建一个ErrorResponseException实例
     *
     * @param statusCode 响应状态码
     * @param message    错误消息
     */
    public ErrorResponseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * 使用响应状态码和指定的原因创建一个ErrorResponseException实例
     *
     * @param statusCode 响应状态码
     * @param cause      异常的原因
     */
    public ErrorResponseException(int statusCode, Throwable cause) {
        super(cause);
        this.statusCode = statusCode;
    }

    /**
     * 使用响应状态码、指定的错误消息和原因创建一个ErrorResponseException实例
     *
     * @param statusCode 响应状态码
     * @param message    错误消息
     * @param cause      异常的原因
     */
    public ErrorResponseException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
