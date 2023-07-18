package com.chestnut.spring.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * 应用上下文工具类
 *
 * @author: Chestnut
 * @since: 2023-07-18
 **/
public class ApplicationContextUtils {
    private static ApplicationContext applicationContext = null;

    /**
     * 获取应用程序上下文。如果应用程序上下文未设置，此方法将抛出一个NullPointerException并带有指定的错误消息
     *
     * @return 应用程序上下文
     */
    @Nonnull
    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(getApplicationContext(), "ApplicationContext is not set.");
    }

    /**
     * 获取应用程序上下文
     *
     * @return 应用程序上下文，如果应用程序上下文未设置，则返回null
     */
    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 设置应用程序上下文
     *
     * @param ctx 应用程序上下文，用于设置应用程序上下文
     */
    public static void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }
}
