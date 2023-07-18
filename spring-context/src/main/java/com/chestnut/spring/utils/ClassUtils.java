package com.chestnut.spring.utils;

import com.chestnut.spring.annotation.Bean;
import com.chestnut.spring.annotation.Component;
import com.chestnut.spring.exception.BeanDefinitionException;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类操作工具类
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class ClassUtils {
    /**
     * 递归查找指定类型的注解
     *
     * @param target    要查找注解的类
     * @param annoClass 注解的类型
     * @param <A>       注解类型
     * @return 找到的注解实例，如果找不到则返回null
     */
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        // 获取指定类型的注解实例
        A a = target.getAnnotation(annoClass);
        // 遍历 target 类上的所有注解
        for (Annotation anno : target.getAnnotations()) {
            // 获取当前注解 anno 的注解类型
            Class<? extends Annotation> annoType = anno.annotationType();
            // Java内置注解直接跳过
            if ("java.lang.annotation".equals(annoType.getPackageName())) {
                continue;
            }
            // 递归调用 findAnnotation 方法，查找当前注解的注解类型 annoType 上是否存在指定类型的注解 annoClass
            A found = findAnnotation(annoType, annoClass);
            if (found != null) {
                // 如果之前已经找到了一个注解实例 a，即出现重复的注解
                if (a != null) {
                    throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName() + " found on class " + target.getSimpleName());
                }
                // 将找到的注解实例 found 赋值给 a
                a = found;
            }
        }
        return a;
    }

    /**
     * 从注解数组中获取指定类型的注解
     *
     * @param annos     注解数组
     * @param annoClass 注解的类型
     * @param <A>       注解类型
     * @return 找到的注解实例，如果找不到则返回null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getAnnotation(Annotation[] annos, Class<A> annoClass) {
        // 遍历注解数组 annos 中的每个注解
        for (Annotation anno : annos) {
            // 检查当前注解 anno 是否是指定类型注解 annoClass 或其子类的实例
            if (annoClass.isInstance(anno)) {
                // 将当前注解 anno 强制转换为指定类型的注解实例 A 并返回
                return (A) anno;
            }
        }
        return null;
    }

    /**
     * 获取@Bean注解标注的方法的bean名称，仅在方法上搜索
     *
     * @param method 带有@Bean注解的方法
     * @return bean名称
     */
    public static String getBeanName(Method method) {
        // 使用 getAnnotation 方法从给定的方法 method 中获取 @Bean 注解的实例
        Bean bean = method.getAnnotation(Bean.class);
        // 使用 value() 方法获取 @Bean 注解的值。如果 @Bean 注解没有显式指定值，则返回空字符串。
        String name = bean.value();
        if (name.isEmpty()) {
            // 如果获取到的 bean 名称为空字符串，则将其设置为方法的名称
            name = method.getName();
        }
        return name;
    }

    /**
     * 获取具有@Component注解标注的类的bean名称
     *
     * @param clazz 带有@Component注解的类
     * @return bean名称
     */
    public static String getBeanName(Class<?> clazz) {
        String name = "";
        // 使用 getAnnotation 方法从给定的类 clazz 中获取 @Component 注解的实例
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            // 如果 @Component 注解实例存在，则获取注解的值
            name = component.value();
        } else {
            // 如果 @Component 注解不存在
            // 遍历类 clazz 上的所有注解
            for (Annotation anno : clazz.getAnnotations()) {
                // 检查当前注解 anno 的注解类型上是否存在 @Component 注解
                if (findAnnotation(anno.annotationType(), Component.class) != null) {
                    try {
                        // 使用反射获取注解类型上的 value() 方法，并调用该方法获取注解的值
                        // 当 invoke() 方法被调用时，它将在 anno 对象上执行 value() 方法
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                        throw new BeanDefinitionException("Cannot get annotation value.", e);
                    }
                }
            }
        }
        // 检查获取到的 bean 名称是否为空字符串
        if (name.isEmpty()) {
            // default name: "HelloWorld" => "helloWorld"
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    /**
     * 查找带有指定注解的方法，不会在父类中搜索
     *
     * @param clazz     要搜索的类
     * @param annoClass 注解类型
     * @return 找到的带有指定注解的非参数方法，如果找不到则返回null
     */
    @Nullable
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass) {
        // 通过调用 getDeclaredMethods() 方法获取类 clazz 中声明的所有方法，并返回一个方法数组
        List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                // 只保留带有指定注解 annoClass 的方法
                .filter(method -> method.isAnnotationPresent(annoClass))
                // 使用 map() 方法对过滤后的方法流进行转换
                .map(method -> {
                    // 注解方法的参数数量总是为0
                    // 检查方法是否有参数，如果有参数，则抛出异常
                    if (method.getParameterCount() != 0) {
                        throw new BeanDefinitionException(String.format("Method '%s' with @%s must not have argument: %s", method.getName(), annoClass.getSimpleName(), clazz.getName()));
                    }
                    return method;
                })
                // 使用 collect() 方法将转换后的方法流收集为一个列表
                .collect(Collectors.toList());
        // 如果为空，表示没有找到带有指定注解的非参数方法，返回 null
        if (methods.isEmpty()) {
            return null;
        }
        // 如果是，表示只找到了一个带有指定注解的非参数方法，返回该方法
        if (methods.size() == 1) {
            return methods.get(0);
        }
        // 如果方法列表中有多个带有指定注解的非参数方法，抛出 BeanDefinitionException 异常，指示在类 clazz 中找到了多个带有指定注解的方法
        throw new BeanDefinitionException(String.format("Multiple methods with @%s found in class: %s", annoClass.getSimpleName(), clazz.getName()));
    }

    /**
     * 根据方法名获取指定类中的方法，不会在父类中搜索
     *
     * @param clazz      要搜索的类
     * @param methodName 方法名
     * @return 找到的方法
     */
    public static Method getNamedMethod(Class<?> clazz, String methodName) {
        try {
            // 使用 getDeclaredMethod() 方法从类 clazz 中获取指定方法名 methodName 的方法对象
            return clazz.getDeclaredMethod(methodName);
        } catch (ReflectiveOperationException e) {
            throw new BeanDefinitionException(String.format("Method '%s' not found in class: %s", methodName, clazz.getName()));
        }
    }
}
