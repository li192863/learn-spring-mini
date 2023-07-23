package com.chestnut.spring.web.utils;

import com.chestnut.spring.context.ApplicationContext;
import com.chestnut.spring.context.ApplicationContextUtils;
import com.chestnut.spring.io.PropertyResolver;
import com.chestnut.spring.utils.ClassPathUtils;
import com.chestnut.spring.utils.YamlUtils;
import com.chestnut.spring.web.DispatcherServlet;
import com.chestnut.spring.web.FilterRegistrationBean;
import jakarta.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * 网络工具类
 *
 * @author: Chestnut
 * @since: 2023-07-21
 **/
public class WebUtils {
    /**
     * 默认参数值
     */
    public static final String DEFAULT_PARAM_VALUE = "\0\t\0\t\0";
    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(WebUtils.class);
    /**
     * 应用配置文件
     */
    private static final String CONFIG_APP_YAML = "/application.yml";
    /**
     * 应用配置属性
     */
    private static final String CONFIG_APP_PROP = "/application.properties";

    /**
     * 在Servlet容器中注册过滤器
     *
     * @param servletContext Servlet上下文对象，用于注册Filter
     */
    public static void registerFilters(ServletContext servletContext) {
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
            // 将 Filter 添加到 Servlet 容器中，并映射到指定的 URL 模式
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
    public static void registerDispatcherServlet(ServletContext servletContext, PropertyResolver propertyResolver) {
        DispatcherServlet dispatcherServlet = new DispatcherServlet(ApplicationContextUtils.getRequiredApplicationContext(), propertyResolver);
        logger.info("register servlet {} for URL '/'", dispatcherServlet.getClass().getName());
        ServletRegistration.Dynamic dispatcherReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        dispatcherReg.addMapping("/");
        // 将在应用程序启动时立即被加载，默认值-1为懒加载
        dispatcherReg.setLoadOnStartup(0);
    }

    /**
     * 创建一个用于解析应用程序配置的PropertyResolver实例
     * 将从/application.yml或/application.properties中读取
     *
     * @return 创建的PropertyResolver实例
     */
    public static PropertyResolver createPropertyResolver() {
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
}
