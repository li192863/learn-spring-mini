package com.chestnut.spring.web;

import com.chestnut.spring.context.AnnotationConfigApplicationContext;
import com.chestnut.spring.context.ApplicationContext;
import com.chestnut.spring.context.ApplicationContextUtils;
import com.chestnut.spring.io.PropertyResolver;
import jakarta.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 内容加载监听器
 *
 * @author: Chestnut
 * @since: 2023-07-24
 **/
public class ContextLoaderInitializer implements ServletContainerInitializer {
    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 配置类
     */
    private final Class<?> configClass;
    /**
     * 属性解析器
     */
    private final PropertyResolver propertyResolver;

    /**
     * 创建一个内容加载监听器
     *
     * @param configClass      配置类
     * @param propertyResolver 属性解析器
     */
    public ContextLoaderInitializer(Class<?> configClass, PropertyResolver propertyResolver) {
        this.configClass = configClass;
        this.propertyResolver = propertyResolver;
    }

    /**
     * 当应用程序启动时，Servlet容器调用此方法
     *
     * @param c   应用程序包含的类的集合
     * @param ctx 应用程序的 ServletContext
     * @throws ServletException 如果在servlet初始化过程中出现错误
     */
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        logger.info("Servlet container start. ServletContext = {}", ctx);
        // 配置请求与响应编码
        String encoding = propertyResolver.getProperty("${spring.web.character-encoding:UTF-8}");
        ctx.setRequestCharacterEncoding(encoding);
        ctx.setResponseCharacterEncoding(encoding);

        // 应用程序上下文 存放 Servlet上下文（WebMvcConfiguration在创建应用程序上下文时被扫描）
        WebMvcConfiguration.setServletContext(ctx);
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(this.configClass, this.propertyResolver);
        logger.info("Application context created: {}", applicationContext);

        // 配置过滤器和调度器
        registerFilters(ctx);
        registerDispatcherServlet(ctx, this.propertyResolver);

    }

    /**
     * 在Servlet容器中注册过滤器
     *
     * @param servletContext Servlet上下文对象，用于注册Filter
     */
    private void registerFilters(ServletContext servletContext) {
        ApplicationContext applicationContext = ApplicationContextUtils.getRequiredApplicationContext();
        // 遍历所有 FilterRegistrationBean 类型的 bean，这些 bean 是在 Spring 配置中定义的过滤器
        for (FilterRegistrationBean filterRegBean : applicationContext.getBeans(FilterRegistrationBean.class)) {
            // 获取过滤器的 URL 模式列表
            List<String> urlPatterns = filterRegBean.getUrlPatterns();
            if (urlPatterns == null || urlPatterns.isEmpty()) {
                throw new IllegalArgumentException("No url patterns for {}" + filterRegBean.getClass().getName());
            }
            // 获取 Filter 实例
            Filter filter = Objects.requireNonNull(filterRegBean.getFilter(), "FilterRegistrationBean.getFilter() must not return null.");
            logger.info("register filter '{}' {} for URLs: {}", filterRegBean.getName(), filter.getClass().getName(), String.join(", ", urlPatterns));
            // 将 Filter 添加到 Servlet 容器中
            FilterRegistration.Dynamic filterReg = servletContext.addFilter(filterRegBean.getName(), filter);
            filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns.toArray(String[]::new));
        }
    }

    /**
     * 在Servlet容器中注册调度器
     *
     * @param servletContext   Servlet上下文对象，用于注册Servlet
     * @param propertyResolver 用于解析Servlet配置属性的PropertyResolver
     */
    private void registerDispatcherServlet(ServletContext servletContext, PropertyResolver propertyResolver) {
        ApplicationContext applicationContext = ApplicationContextUtils.getRequiredApplicationContext();
        // 获取 DispatcherServlet 实例
        DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext, propertyResolver);
        logger.info("register servlet {} for URL '/'", dispatcherServlet.getClass().getName());
        // 将 DispatcherServlet 添加到 Servlet 容器中
        ServletRegistration.Dynamic dispatcherReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        dispatcherReg.addMapping("/");
        // 将在应用程序启动时立即被加载，默认值-1为懒加载
        dispatcherReg.setLoadOnStartup(0);
    }
}
