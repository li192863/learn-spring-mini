package com.chestnut.spring.web.utils;

import jakarta.servlet.ServletException;

import java.util.regex.Pattern;

/**
 * 路径工具类
 *
 * @author: Chestnut
 * @since: 2023-07-22
 **/
public class PathUtils {
    /**
     * 传入的路径参数转换成一个正则表达式的模式
     *
     * @param path 路径参数
     * @return Pattern对象，用于编译路径字符串为正则表达式
     * @throws ServletException 如果路径非法
     */
    public static Pattern compile(String path) throws ServletException {
        // "{变量名}"替换为"(?<$1>[^/]*)"，"(?<$1>[^/]*)"可捕获分组，通过matcher.matches()方法后matcher.group("变量名")获取其捕获值
        // "/users/{id}/{profile}" -> "/users/(?<id>[^/]*)/(?<profile>[^/]*)"
        // [^/]*将匹配路径中/users/和/profile之间的任何内容，变量可以是任何不包含斜杠/的字符序列，比如123, abc, user123等
        // ?<id>将捕获的内容存储，可通过matcher.matches()方法后使用matcher.group("变量名")获取其捕获值
        String regPath = path.replaceAll("\\{([a-zA-Z][a-zA-Z\\d]*)}", "(?<$1>[^/]*)");
        // 检查regPath中是否还有未被替换的{}形式的部分
        if (regPath.indexOf('{') >= 0 || regPath.indexOf('}') >= 0) {
            throw new ServletException("Invalid path: " + path);
        }
        // 将regPath拼接成一个完整的正则表达式
        return Pattern.compile("^" + regPath + "$");
    }
}
