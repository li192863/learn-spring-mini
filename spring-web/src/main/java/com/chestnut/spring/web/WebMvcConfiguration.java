package com.chestnut.spring.web;

import com.chestnut.spring.annotation.Autowired;
import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.annotation.Value;
import jakarta.servlet.ServletContext;

import java.util.Objects;

/**
 * 模型视图控制配置类
 *
 * @author: Chestnut
 * @since: 2023-07-22
 **/
@Configuration
public class WebMvcConfiguration {
    /**
     * Servlet上下文，用于获取资源信息
     */
    private static ServletContext servletContext = null;

    /**
     * 设置Servlet上下文，由监听器设置
     *
     * @param ctx Servlet上下文
     */
    public static void setServletContext(ServletContext ctx) {
        servletContext = ctx;
    }

    /**
     * Servlet上下文工厂方法
     *
     * @return Servlet上下文
     */
    @Bean
    public ServletContext servletContext() {
        return Objects.requireNonNull(servletContext, "ServletContext is not set.");
    }

    /**
     * 视图解析器工厂方法
     *
     * @param servletContext   Servlet上下文
     * @param templatePath     FreeMarker模板文件路径
     * @param templateEncoding FreeMarker模板文件编码方式
     * @return 视图解析器
     */
    @Bean(initMethod = "init")
    public ViewResolver viewResolver(
            @Autowired ServletContext servletContext,
            @Value("${spring.web.freemarker.template-path:/WEB-INF/templates}") String templatePath,
            @Value("${spring.web.freemarker.template-encoding:UTF-8}") String templateEncoding
    ) {
        return new FreeMarkerViewResolver(servletContext, templatePath, templateEncoding);
    }
}
