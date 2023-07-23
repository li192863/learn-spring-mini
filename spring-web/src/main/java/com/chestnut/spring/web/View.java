package com.chestnut.spring.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * 视图接口
 */
public interface View {
    /**
     * 获取内容类型，表示当前对象的内容类型
     *
     * @return 内容类型
     */
    @Nullable
    default String getContentType() {
        return null;
    }

    /**
     * 渲染视图，并输出渲染后的内容到HTTP响应中
     *
     * @param model    视图模型数据，包含模板渲染所需的变量和值
     * @param request  HTTP请求对象，可以用于获取请求信息
     * @param response HTTP响应对象，用于输出渲染后的内容
     * @throws Exception 如果在渲染过程中发生异常
     */
    void render(@Nullable Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
