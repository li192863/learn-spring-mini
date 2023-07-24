package com.chestnut.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 前置处理器抽象类
 *
 * @author: Chestnut
 * @since: 2023-07-19
 **/
public abstract class BeforeInvocationHandlerAdapter implements InvocationHandler {
    /**
     * 定义代理对象方法调用前的额外逻辑
     *
     * @param proxy  代理对象
     * @param method 被调用的方法对象
     * @param args   调用方法时传递的参数数组
     */
    public abstract void before(Object proxy, Method method, Object[] args);

    /**
     * 代理对象的方法被调用时执行，在原始方法执行前执行before方法
     *
     * @param proxy  代理对象
     * @param method 被调用的方法对象
     * @param args   调用方法时传递的参数数组
     * @return 原始方法返回值
     * @throws Throwable invoke过程中抛出异常
     */
    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 实际方法调用前执行的逻辑
        before(proxy, method, args);
        // 原始方法
        return method.invoke(proxy, args);
    }
}
