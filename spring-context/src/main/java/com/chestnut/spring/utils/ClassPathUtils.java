package com.chestnut.spring.utils;

import com.chestnut.spring.io.InputStreamCallback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * 类路径工具类
 *
 * @author: Chestnut
 * @since: 2023-07-13
 **/
public class ClassPathUtils {
    /**
     * 从指定路径读取输入流，并通过输入流回调执行特定操作，并返回操作结果
     *
     * @param path                输入流的路径
     * @param inputStreamCallback 输入流回调对象
     * @param <T>                 操作结果的类型
     * @return 操作结果
     */
    public static <T> T readInputStream(String path, InputStreamCallback<T> inputStreamCallback) {
        // 去除路径开头的斜杠
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try (InputStream input = getContextClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new FileNotFoundException("File not found in classpath: " + path);
            }
            // 执行输入流回调操作，并返回操作结果
            return inputStreamCallback.doWithInputStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 从指定路径读取文本文件内容并以字符串形式返回
     *
     * @param path 文件路径
     * @return 文件内容的字符串表示
     */
    public static String readString(String path) {
        return readInputStream(path, inputStream -> {
            // 读取输入流中的所有字节并以UTF-8编码转换为字符串
            byte[] data = inputStream.readAllBytes();
            return new String(data, StandardCharsets.UTF_8);
        });
    }

    /**
     * 获取上下文类加载器
     *
     * @return 当前线程的上下文类加载器
     */
    private static ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        // 获取当前线程的上下文类加载器
        cl = Thread.currentThread().getContextClassLoader();
        // 若未指定特定的上下文类加载器
        if (cl == null) {
            // 获取当前类（即包含该方法的类）的类加载器
            cl = ClassPathUtils.class.getClassLoader();
        }
        return cl;
    }
}
