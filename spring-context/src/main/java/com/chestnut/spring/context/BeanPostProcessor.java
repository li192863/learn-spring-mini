package com.chestnut.spring.context;

/**
 * 后处理Bean接口
 */
public interface BeanPostProcessor {
    /**
     * 在给定bean初始化之前应用此BeanPostProcessor
     * 在该方法中应保存原始Bean，可创建代理Bean替换原始Bean，并应返回bean
     *
     * @param bean     正在初始化的bean实例
     * @param beanName bean的名称
     * @return 可能被修改后的bean实例
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 在给定bean初始化之后应用此BeanPostProcessor
     *
     * @param bean     已经初始化的bean实例
     * @param beanName bean的名称
     * @return 可能被修改后的bean实例
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 在调用任何bean的setter方法之前应用此BeanPostProcessor
     * 在该方法中应获取原始Bean，可返回原始未被代理的Bean，并应返回bean
     *
     * @param bean     将要设置属性的bean实例
     * @param beanName bean的名称
     * @return 可能被修改后的bean实例
     */
    default Object postProcessOnSetProperty(Object bean, String beanName) {
        return bean;
    }
}
