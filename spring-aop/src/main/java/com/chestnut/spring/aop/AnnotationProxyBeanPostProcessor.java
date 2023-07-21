package com.chestnut.spring.aop;

import com.chestnut.spring.context.ApplicationContextUtils;
import com.chestnut.spring.context.BeanDefinition;
import com.chestnut.spring.context.BeanPostProcessor;
import com.chestnut.spring.context.ConfigurableApplicationContext;
import com.chestnut.spring.exception.AopConfigException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 注解代理Bean后处理器
 *
 * @author: Chestnut
 * @since: 2023-07-19
 **/
public class AnnotationProxyBeanPostProcessor<A extends Annotation> implements BeanPostProcessor {
    /**
     * 原始Bean集合
     */
    private Map<String, Object> originBeans = new HashMap<>();
    /**
     * 当前类的直接父类的泛型参数类型
     * 例如：public class TransactionalProxyBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {}
     * 其annotationClass为Transactional.class，本质为注解类实例
     */
    private Class<A> annotationClass;

    /**
     * 创建一个AnnotationProxyBeanPostProcessor实例
     */
    public AnnotationProxyBeanPostProcessor() {
        this.annotationClass = getParameterizedType();
    }

    /**
     * 在给定bean初始化之前应用此BeanPostProcessor，用于在目标对象上添加代理
     *
     * @param bean     要代理的对象实例，或其他
     * @param beanName 要代理的对象名称，或其他
     * @return 如果检测到目标对象类上存在指定的类级别注解，则返回代理对象，否则返回原始目标对象
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // 获取要代理的对象的类
        Class<?> beanClass = bean.getClass();
        // 检查是否有类级别注解
        A anno = beanClass.getAnnotation(this.annotationClass);
        // 如果检测到目标对象类上存在指定的类级别注解，则返回代理对象
        if (anno != null) {
            // 代理处理器的名称
            String handlerName;
            try {
                // 获取代理处理器的名称，例：获取@Around("aroundInvocationHandler")中的"aroundInvocationHandler"
                handlerName = (String) anno.annotationType().getMethod("value").invoke(anno);
            } catch (ReflectiveOperationException e) {
                throw new AopConfigException(String.format("@%s must have value() returned String type.", this.annotationClass.getSimpleName()), e);
            }
            // 创建代理对象
            Object proxy = createProxy(beanClass, bean, handlerName);
            // 保存要被代理的对象实例
            this.originBeans.put(beanName, bean);
            // 返回代理对象
            return proxy;
        }
        // 返回原始目标对象
        return bean;
    }

    /**
     * 创建一个代理对象
     *
     * @param beanClass   要代理的对象的类
     * @param bean        要代理的对象实例
     * @param handlerName 代理处理器的名称
     * @return 代理对象
     */
    private Object createProxy(Class<?> beanClass, Object bean, String handlerName) {
        // 获取Spring容器的应用上下文（ApplicationContext）对象
        ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) ApplicationContextUtils.getRequiredApplicationContext();
        // 在应用上下文中找到对应的Spring Bean定义
        BeanDefinition handlerDef = ctx.findBeanDefinition(handlerName);
        // 检查是否找到了代理处理器的Bean定义
        if (handlerDef == null) {
            throw new AopConfigException(String.format("@%s proxy handler '%s' not found.", this.annotationClass.getSimpleName(), handlerName));
        }
        // 获取代理处理器的实例
        Object handlerBean = handlerDef.getInstance();
        // 检查代理处理器是否已初始化
        if (handlerBean == null) {
            handlerBean = ctx.createBeanAsEarlySingleton(handlerDef);
        }
        // 检查代理处理器是InvocationHandler类型
        if (handlerBean instanceof InvocationHandler handler) {
            // 创建代理对象，代理bean，并指定代理对象的调用处理器为handler（代理处理器）
            return ProxyResolver.getInstance().createProxy(bean, handler);
        } else {
            throw new AopConfigException(String.format("@%s proxy handler '%s' is not type of %s.", this.annotationClass.getSimpleName(), handlerName, InvocationHandler.class.getName()));
        }

    }

    /**
     * 在调用任何bean的setter方法之前应用此BeanPostProcessor，用于在目标对象上设置属性
     * 在该方法中应获取原始Bean，可返回原始未被代理的Bean，并应返回bean
     *
     * @param bean     被代理的对象实例，也就是代理类实例，或其他
     * @param beanName 被代理的对象名称，或其他
     * @return 如果检测到目标对象类被代理类所代理，则返回代理类的原始对象（代理类的父类），否则返回目标对象
     */
    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = this.originBeans.get(beanName);
        return origin != null ? origin : bean;
    }

    /**
     * 获取当前类的直接父类的泛型参数类型
     * 该方法假设当前类有且仅有一个直接父类，并且这个父类是一个泛型类，其中只有一个泛型参数
     *
     * @return 当前类的直接父类的泛型参数类型
     */
    @SuppressWarnings("unchecked")
    private Class<A> getParameterizedType() {
        // 获取当前类的直接父类的类型信息
        Type type = getClass().getGenericSuperclass();
        // 检查父类的类型信息是否是ParameterizedType的实例，如果不是，抛出一个异常，表示当前类没有使用泛型
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type.");
        }
        // 获取到的类型信息强制转换为ParameterizedType类型
        ParameterizedType pt = (ParameterizedType) type;
        // 获取参数化类型的实际类型参数数组，因为泛型可能有多个参数
        Type[] types = pt.getActualTypeArguments();
        // 检查实际类型参数数组的长度
        if (types.length != 1) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type.");
        }
        // 获取实际类型参数数组的第一个元素，也就是泛型参数的类型信息
        Type r = types[0];
        // 检查泛型参数的类型信息是否为Class<?>的实例
        if (!(r instanceof Class<?>)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type of class.");
        }
        return (Class<A>) r;
    }
}
