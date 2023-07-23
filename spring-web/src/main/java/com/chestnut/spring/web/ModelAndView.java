package com.chestnut.spring.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型视图类
 *
 * @author: Chestnut
 * @since: 2023-07-22
 **/
public class ModelAndView {
    /**
     * 视图名称
     */
    private String view;
    /**
     * 模型数据
     */
    private Map<String, Object> model;
    /**
     * 状态码
     */
    private int status;

    /**
     * 创建一个ModelAndView对象，设置视图名称和默认状态码为SC_OK(200)
     *
     * @param viewName 视图名称
     */
    public ModelAndView(String viewName) {
        this(viewName, HttpServletResponse.SC_OK, null);
    }

    /**
     * 创建一个ModelAndView对象，设置视图名称和模型数据，并默认状态码为SC_OK(200)
     *
     * @param viewName 视图名称
     * @param model    视图模型数据，包含模板渲染所需的变量和值
     */
    public ModelAndView(String viewName, @Nullable Map<String, Object> model) {
        this(viewName, HttpServletResponse.SC_OK, model);
    }

    /**
     * 创建一个ModelAndView对象，设置视图名称，并添加单个模型数据项，默认状态码为SC_OK(200)
     *
     * @param viewName    视图名称
     * @param modelName   模型数据的名称，表示模型中的键
     * @param modelObject 模型数据的对象，表示模型中的值
     */
    public ModelAndView(String viewName, String modelName, Object modelObject) {
        this(viewName, HttpServletResponse.SC_OK, null);
        addModel(modelName, modelObject);
    }

    /**
     * 创建一个ModelAndView对象，设置视图名称和状态码，并默认模型数据为空
     *
     * @param viewName 视图名称
     * @param status   HTTP状态码，表示响应的状态
     */
    public ModelAndView(String viewName, int status) {
        this(viewName, status, null);
    }

    /**
     * 创建一个ModelAndView对象，设置视图名称、状态码和模型数据
     *
     * @param viewName 视图名称
     * @param status   HTTP状态码，表示响应的状态
     * @param model    视图模型数据，包含模板渲染所需的变量和值
     */
    public ModelAndView(String viewName, int status, @Nullable Map<String, Object> model) {
        this.view = viewName;
        this.status = status;
        if (model != null) {
            addModel(model);
        }
    }

    /**
     * 添加模型数据，将指定的模型数据map添加到当前模型数据中
     *
     * @param map 要添加的模型数据map
     */
    public void addModel(Map<String, Object> map) {
        if (this.model == null) {
            this.model = new HashMap<>();
        }
        this.model.putAll(map);
    }

    /**
     * 添加单个模型数据项，将指定的模型数据项添加到当前模型数据中
     *
     * @param key   模型数据项的键
     * @param value 模型数据项的值
     */
    public void addModel(String key, Object value) {
        if (this.model == null) {
            this.model = new HashMap<>();
        }
        this.model.put(key, value);
    }

    /**
     * 获取模型数据，如果当前模型数据为null，则创建一个新的空模型数据并返回
     *
     * @return 模型数据
     */
    public Map<String, Object> getModel() {
        if (this.model == null) {
            this.model = new HashMap<>();
        }
        return this.model;
    }

    /**
     * 获取视图名称
     *
     * @return 视图名称
     */
    public String getViewName() {
        return this.view;
    }

    /**
     * 获取状态码
     *
     * @return 状态码，表示响应的状态
     */
    public int getStatus() {
        return this.status;
    }
}
