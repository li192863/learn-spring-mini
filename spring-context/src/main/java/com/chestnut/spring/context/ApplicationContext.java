package com.chestnut.spring.context;

import java.util.List;

/**
 * 给用户使用的ApplicationContext接口
 **/
public interface ApplicationContext extends AutoCloseable {
    /**
     * 判断容器中是否包含指定名称的Bean
     *
     * @param name 要检查的Bean名称
     * @return 如果容器中包含指定名称的Bean，则返回true；否则返回false
     */
    boolean containsBean(String name);

    /**
     * 根据指定类型获取所有符合条件的Bean实例列表，如果未找到，则返回空列表
     *
     * @param requiredType 要获取的Bean类型
     * @param <T>          Bean的类型
     * @return 指定类型的Bean实例列表
     */
    <T> List<T> getBeans(Class<T> requiredType);

    /**
     * 根据指定的名称获取对应的Bean实例，如果未找到，则抛出异常
     *
     * @param name 要获取的Bean的名称
     * @param <T>  Bean的类型
     * @return 指定名称对应的Bean实例
     */
    <T> T getBean(String name);

    /**
     * 根据指定的名称和类型获取对应的Bean实例，如果未找到，则抛出异常
     *
     * @param name         要获取的Bean的名称
     * @param requiredType 要获取的Bean类型
     * @param <T>          Bean的类型
     * @return 指定名称和类型对应的Bean实例
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * 根据指定类型获取对应的Bean实例，如果未找到，则抛出异常
     *
     * @param requiredType 要获取的Bean类型
     * @param <T>          Bean的类型
     * @return 指定类型对应的Bean实例
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * 关闭并执行所有bean的destroy方法
     */
    void close();
}
