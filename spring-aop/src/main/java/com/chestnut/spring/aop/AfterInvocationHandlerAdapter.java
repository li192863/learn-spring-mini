package com.chestnut.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 后置处理器抽象类
 *
 * @author: Chestnut
 * @since: 2023-07-19
 **/
public abstract class AfterInvocationHandlerAdapter implements InvocationHandler {
    /**
     * 定义代理对象方法调用后的额外逻辑，after允许修改方法返回值
     *
     * @param proxy       代理对象
     * @param returnValue 原始方法调用后的返回值
     * @param method      被调用的方法对象
     * @param args        调用方法时传递的参数数组
     * @return 自定义返回值
     */
    public abstract Object after(Object proxy, Object returnValue, Method method, Object[] args);

    /**
     * 代理对象的方法被调用时执行，在原始方法执行后执行after方法
     *
     * @param proxy  代理对象
     * @param method 被调用的方法对象
     * @param args   调用方法时传递的参数数组
     * @return after方法返回值
     * @throws Throwable invoke过程中抛出异常
     */
    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 原始方法
        Object ret = method.invoke(proxy, args);
        // 实际方法调用后执行的逻辑
        return after(proxy, ret, method, args);
    }
}
