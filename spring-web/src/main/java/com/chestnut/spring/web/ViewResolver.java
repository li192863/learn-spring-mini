package com.chestnut.spring.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * 视图解析器
 */
public interface ViewResolver {
    /**
     * 初始化视图解析器
     */
    void init();

    /**
     * 渲染视图的方法，将指定的视图名和模型数据渲染成实际的响应内容并发送给客户端
     *
     * @param viewName 视图名，用于表示要渲染的前端视图的逻辑名称
     * @param model    模型数据，一个包含键值对的Map对象，用于传递给视图进行渲染
     * @param req      客户端发送的HTTP请求对象
     * @param resp     服务端发送的HTTP响应对象
     * @throws ServletException 如果在Servlet处理过程中发生异常
     * @throws IOException      如果在处理请求和响应时发生I/O异常
     */
    void render(String viewName, Map<String, Object> model, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
}
