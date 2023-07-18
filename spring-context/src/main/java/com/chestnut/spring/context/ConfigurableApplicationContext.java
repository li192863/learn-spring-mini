package com.chestnut.spring.context;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Framework级别的代码用的ConfigurableApplicationContext接口
 */
public interface ConfigurableApplicationContext extends ApplicationContext {
    /**
     * 根据指定类型获取所有符合条件的Bean定义列表，如果未找到，则返回空列表
     *
     * @param type 要获取的Bean定义类型
     * @return 指定类型的Bean定义列表
     */
    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    /**
     * 根据指定的名称获取对应的Bean定义，如果未找到，则返回null
     *
     * @param name 要获取的Bean定义的名称
     * @return 指定名称对应的Bean定义
     */
    @Nullable
    BeanDefinition findBeanDefinition(String name);

    /**
     * 根据指定的名称和类型获取对应的Bean定义，如果未找到，则返回null，如果找到但类型不匹配，则抛出异常
     *
     * @param name         要获取的Bean的定义名称
     * @param requiredType 要获取的Bean定义类型
     * @return 指定名称和类型对应的Bean定义
     */
    @Nullable
    BeanDefinition findBeanDefinition(String name, Class<?> requiredType);

    /**
     * 根据指定类型获取对应的Bean定义，如果未找到，则返回null
     * 如果存在多个，则优先选择标记了@Primary注解的定义；
     * 如果存在多个@Primary注解，或者没有@Primary注解但存在多个定义，则抛出NoUniqueBeanDefinitionException异常
     *
     * @param type 要获取的Bean定义类型
     * @return 指定类型对应的Bean定义
     */
    @Nullable
    BeanDefinition findBeanDefinition(Class<?> type);

    /**
     * 创建一个Bean，但不进行字段和方法级别的注入。如果创建的Bean不是Configuration或BeanPostProcessor，则在构造方法中注入的依赖Bean会自动创建。
     *
     * @param def Bean的定义
     * @return Bean的实例
     */
    Object createBeanAsEarlySingleton(BeanDefinition def);
}
