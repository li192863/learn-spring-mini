package com.chestnut.spring.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 输入流回调接口，用于在处理输入流时执行特定操作，并返回结果
 *
 * @author: Chestnut
 * @since: 2023-07-13
 **/
@FunctionalInterface
public interface InputStreamCallback<T> {
    /**
     * 在给定的输入流上执行特定操作，并返回结果
     * @param inputStream 输入流
     * @return 操作结果
     * @throws IOException 如果在操作过程中发生I/O错误
     */
    T doWithInputStream(InputStream inputStream) throws IOException;
}
