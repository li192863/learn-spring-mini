package com.chestnut.spring.aop;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;

/**
 * 代理解析器，用于创建动态代理对象
 * Create proxy by subclassing and override methods with interceptor.
 *
 * @author: Chestnut
 * @since: 2023-07-19
 **/
public class ProxyResolver {
    /**
     * 日志
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * ByteBuddy 对象，用于创建代理类
     */
    private final ByteBuddy byteBuddy = new ByteBuddy();
    /**
     * 代理解析器实例
     */
    private static ProxyResolver INSTANCE = null;

    /**
     * 创建一个ProxyResolver实例，禁止外部访问
     */
    private ProxyResolver() {
    }

    /**
     * 获取代理解析器实例
     *
     * @return 代理解析器实例
     */
    public static ProxyResolver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProxyResolver();
        }
        return INSTANCE;
    }

    /**
     * 创建动态代理对象的方法
     *
     * @param bean    原始Bean，要被代理的目标对象
     * @param handler 拦截器，代理对象的调用处理器
     * @param <T>     代理对象的类型参数
     * @return 代理后的实例，动态代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T bean, InvocationHandler handler) {
        // 获取目标Bean的类信息
        Class<?> targetClass = bean.getClass();
        logger.atDebug().log("create proxy for bean {} @{}", targetClass.getName(), Integer.toHexString(bean.hashCode()));
        // 使用ByteBuddy动态创建Proxy的Class
        Class<?> proxyClass = this.byteBuddy
                // 子类用默认无参数构造方法，This strategy is adding a default constructor that calls its super types default constructor.
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                // 拦截/代理所有public方法
                .method(ElementMatchers.isPublic())
                // 设置代理对象的调用处理器，使用传入的 handler 来处理代理方法的调用
                .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> handler.invoke(bean, method, args)))
                // 编译生成代理类
                .make()
                // 加载代理类
                .load(targetClass.getClassLoader())
                // 获取代理类
                .getLoaded();
        // 创建代理对象
        Object proxy = null;
        try {
            // 通过反射获取代理类的默认构造函数，并创建代理对象
            proxy = proxyClass.getConstructor().newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return (T) proxy;
    }
}
