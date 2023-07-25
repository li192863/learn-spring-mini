package com.chestnut.spring.web;

import com.chestnut.spring.context.AnnotationConfigApplicationContext;
import com.chestnut.spring.context.ApplicationContext;
import com.chestnut.spring.context.ApplicationContextUtils;
import com.chestnut.spring.exception.NestedRuntimeException;
import com.chestnut.spring.io.PropertyResolver;
import com.chestnut.spring.utils.ClassPathUtils;
import com.chestnut.spring.utils.YamlUtils;
import jakarta.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.*;

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
     * 应用配置文件
     */
    private static final String CONFIG_APP_YAML = "/application.yml";
    /**
     * 应用配置属性
     */
    private static final String CONFIG_APP_PROP = "/application.properties";

    /**
     * 当ServletContext初始化时，将自动调用此方法
     *
     * @param sce the ServletContextEvent containing the ServletContext that is being initialized
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("init {}.", getClass().getName());
        // 获取属性解析器
        PropertyResolver propertyResolver = createPropertyResolver();
        // 获取Servlet上下文
        ServletContext servletContext = sce.getServletContext();

        // 应用程序上下文 存放 Servlet上下文（WebMvcConfiguration在创建应用程序上下文时被扫描）
        WebMvcConfiguration.setServletContext(servletContext);

        // 配置请求与响应编码
        String encoding = propertyResolver.getProperty("${spring.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);

        // 获取应用程序上下文
        String configClassName = servletContext.getInitParameter("configuration");
        ApplicationContext applicationContext = createApplicationContext(configClassName, propertyResolver);

        // 配置过滤器和调度器
        registerFilters(servletContext);
        registerDispatcherServlet(servletContext, propertyResolver);

        // Servlet上下文 存放 应用程序上下文
        servletContext.setAttribute("applicationContext", applicationContext);
    }


    /**
     * 创建一个属性解析器
     * 将从/application.yml或/application.properties中读取
     *
     * @return 属性解析器
     */
    private PropertyResolver createPropertyResolver() {
        final Properties props = new Properties();
        try {
            // 尝试加载application.yml
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            logger.info("load config: {}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if (value instanceof String strValue) {
                    props.put(key, strValue);
                }
            }
        } catch (UncheckedIOException e) {
            // 尝试加载application.properties
            if (e.getCause() instanceof FileNotFoundException) {
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, inputStream -> {
                    logger.info("load config: {}", CONFIG_APP_PROP);
                    props.load(inputStream);
                    return true;
                });
            }
        }
        return new PropertyResolver(props);
    }


    /**
     * 创建一个应用程序上下文
     *
     * @param configClassName  配置类名称
     * @param propertyResolver 属性解析器
     * @return 应用程序上下文
     */
    private ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver) {
        logger.info("init ApplicationContext by configuration: {}", configClassName);
        // 若配置类为空则抛出异常
        if (configClassName == null || configClassName.isEmpty()) {
            throw new NestedRuntimeException("Cannot init ApplicationContext for missing init param name: configuration");
        }
        // 尝试加载配置类
        Class<?> configClass;
        try {
            configClass = Class.forName(configClassName);
        } catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("Could not load class from init param 'configuration': " + configClassName);
        }
        return new AnnotationConfigApplicationContext(configClass, propertyResolver);
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
