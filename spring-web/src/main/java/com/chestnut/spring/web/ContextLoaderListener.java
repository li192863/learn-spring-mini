package com.chestnut.spring.web;

import com.chestnut.spring.context.AnnotationConfigApplicationContext;
import com.chestnut.spring.context.ApplicationContext;
import com.chestnut.spring.exception.NestedRuntimeException;
import com.chestnut.spring.io.PropertyResolver;
import com.chestnut.spring.web.utils.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内容加载监听器
 *
 * @author: Chestnut
 * @since: 2023-07-21
 **/
public class ContextLoaderListener implements ServletContextListener {
    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 当ServletContext初始化时，将自动调用此方法
     *
     * @param sce the ServletContextEvent containing the ServletContext that is being initialized
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("init {}.", getClass().getName());
        // 设置Servlet上下文
        ServletContext servletContext = sce.getServletContext();
        WebMvcConfiguration.setServletContext(servletContext);

        // 获取属性解析器
        PropertyResolver propertyResolver = WebUtils.createPropertyResolver();

        // 设置请求与响应编码
        String encoding = propertyResolver.getProperty("${spring.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);

        // 获取应用程序上下文
        ApplicationContext applicationContext = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);

        // 注册过滤器
        WebUtils.registerFilters(servletContext);
        // 注册调度器
        WebUtils.registerDispatcherServlet(servletContext, propertyResolver);
        // 设置应用程序上下文
        servletContext.setAttribute("applicationContext", applicationContext);
    }

    /**
     * 创建应用程序上下文
     *
     * @param configClassName  配置类名称
     * @param propertyResolver 属性解析器
     * @return 应用程序上下文
     */
    private ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver) {
        logger.info("init ApplicationContext by configuration: {}", configClassName);
        if (configClassName == null || configClassName.isEmpty()) {
            throw new NestedRuntimeException("Cannot init ApplicationContext for missing init param name: configuration");
        }
        Class<?> configClass;
        try {
            configClass = Class.forName(configClassName);
        } catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("Could not load class from init param 'configuration': " + configClassName);
        }
        return new AnnotationConfigApplicationContext(configClass, propertyResolver);
    }
}
