package com.chestnut.spring.web;

import jakarta.servlet.Filter;

import java.util.List;

/**
 * 过滤器注册Bean
 *
 * @author: Chestnut
 * @since: 2023-07-22
 **/
public abstract class FilterRegistrationBean {
    /**
     * 获取过滤器的URL模式列表
     *
     * @return 过滤器的URL模式列表
     */
    public abstract List<String> getUrlPatterns();

    /**
     * 获取Filter实例
     *
     * @return Filter实例
     */
    public abstract Filter getFilter();

    /**
     * 获取类的名称，小驼峰处理，并去除末尾的FilterRegistrationBean或FilterRegistration
     * 举例：
     * ApiFilterRegistrationBean -> apiFilter
     * ApiFilterRegistration -> apiFilter
     * ApiFilterReg -> apiFilterReg
     *
     * @return 类名
     */
    public String getName() {
        String name = getClass().getSimpleName();
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        if (name.endsWith("FilterRegistrationBean") && name.length() > "FilterRegistrationBean".length()) {
            return name.substring(0, name.length() - "FilterRegistrationBean".length());
        }
        if (name.endsWith("FilterRegistration") && name.length() > "FilterRegistration".length()) {
            return name.substring(0, name.length() - "FilterRegistration".length());
        }
        return name;
    }
}
