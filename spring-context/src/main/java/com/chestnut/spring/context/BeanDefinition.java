package com.chestnut.spring.context;

import com.chestnut.spring.exception.BeanCreationException;
import jakarta.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Bean定义类
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class BeanDefinition implements Comparable<BeanDefinition> {
    /**
     * 全局唯一的Bean名称
     */
    private final String name;
    /**
     * Bean的声明类型
     * 对于 @Component 定义的Bean，它的声明类型就是其Class本身
     * 对于 @Bean 定义的Bean，它的声明类型与实际类型不一定是同一类型
     */
    private final Class<?> beanClass;
    /**
     * Bean的实例
     */
    private Object instance = null;
    /**
     * 构造方法/null，包括私有/默认构造函数
     */
    private final Constructor<?> constructor;
    /**
     * 工厂方法名称/null，通常为 "XyzConfiguration"
     */
    private final String factoryName;
    /**
     * 工厂方法/null，通常为 @Bean 标注的一个方法
     */
    private final Method factoryMethod;
    /**
     * Bean的顺序
     */
    private final int order;
    /**
     * 是否标识@Primary
     */
    private final boolean primary;

    /**
     * 初始方法名称
     */
    private String initMethodName;
    /**
     * 销毁方法名称
     */
    private String destroyMethodName;

    /**
     * 初始方法
     */
    private Method initMethod;
    /**
     * 销毁方法
     */
    private Method destroyMethod;

    /**
     * 构造函数方式创建Bean
     *
     * @param name              全局唯一的Bean名称
     * @param beanClass         Bean的声明类型
     * @param constructor       构造方法
     * @param order             Bean的顺序
     * @param primary           是否标识@Primary
     * @param initMethodName    初始方法名称
     * @param destroyMethodName 销毁方法名称
     * @param initMethod        初始方法
     * @param destroyMethod     销毁方法
     */
    public BeanDefinition(String name, Class<?> beanClass, Constructor<?> constructor, int order, boolean primary,
                          String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        // 构造函数，包括私有/默认构造函数
        this.constructor = constructor;
        this.factoryName = null;
        this.factoryMethod = null;
        this.order = order;
        this.primary = primary;
        constructor.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    /**
     * 工厂方法方式创建Bean
     *
     * @param name              全局唯一的Bean名称
     * @param beanClass         Bean的声明类型
     * @param factoryName       工厂方法名称
     * @param factoryMethod     工厂方法
     * @param order             Bean的顺序
     * @param primary           是否标识@Primary
     * @param initMethodName    初始方法名称
     * @param destroyMethodName 销毁方法名称
     * @param initMethod        初始方法
     * @param destroyMethod     销毁方法
     */
    public BeanDefinition(String name, Class<?> beanClass, String factoryName, Method factoryMethod, int order, boolean primary,
                          String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = null;
        // 通常为 "XyzConfiguration"
        this.factoryName = factoryName;
        // 通常为 @Bean 标注的一个方法
        this.factoryMethod = factoryMethod;
        this.order = order;
        this.primary = primary;
        factoryMethod.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    /**
     * 设置初始方法和销毁方法，存储在BeanDefinition的方法名称与方法，其中总有一个为null
     * 对于构造方法构建，初始/销毁方法名称必为null，可能存在初始/销毁方法
     * 对于工厂方法构建，初始/销毁方法必为null，可能存在初始/销毁方法名称
     *
     * @param initMethodName    初始方法名称
     * @param destroyMethodName 销毁方法名称
     * @param initMethod        初始方法
     * @param destroyMethod     销毁方法
     */
    private void setInitAndDestroyMethod(String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
        // 检查传入的初始化方法 initMethod 是否为非空
        if (initMethod != null) {
            // 将初始化方法的可访问性设置为 true，以便在需要时可以通过反射访问该方法
            initMethod.setAccessible(true);
        }
        // 检查传入的销毁方法 destroyMethod 是否为非空
        if (destroyMethod != null) {
            // 将销毁方法的可访问性设置为 true，以便在需要时可以通过反射访问该方法
            destroyMethod.setAccessible(true);
        }
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    /**
     * 获取构造方法对象
     *
     * @return 构造方法对象
     */
    @Nullable
    public Constructor<?> getConstructor() {
        return this.constructor;
    }

    /**
     * 获取工厂方法名称，通常为 "XyzConfiguration"
     *
     * @return 工厂方法名称
     */
    @Nullable
    public String getFactoryName() {
        return this.factoryName;
    }

    /**
     * 获取工厂方法对象，通常为 @Bean 标注的一个方法
     *
     * @return 工厂方法对象
     */
    @Nullable
    public Method getFactoryMethod() {
        return this.factoryMethod;
    }

    /**
     * 获取初始方法对象
     *
     * @return 初始方法对象
     */
    @Nullable
    public Method getInitMethod() {
        return this.initMethod;
    }

    /**
     * 获取销毁方法对象
     *
     * @return 销毁方法对象
     */
    @Nullable
    public Method getDestroyMethod() {
        return this.destroyMethod;
    }

    /**
     * 获取初始方法名称
     *
     * @return 初始方法名称
     */
    @Nullable
    public String getInitMethodName() {
        return this.initMethodName;
    }

    /**
     * 获取销毁方法名称
     *
     * @return 销毁方法名称
     */
    @Nullable
    public String getDestroyMethodName() {
        return this.destroyMethodName;
    }

    /**
     * 获取Bean的名字
     *
     * @return Bean的名字
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取Bean的声明类型
     * 对于 @Component 定义的Bean，它的声明类型就是其Class本身
     * 对于 @Bean 定义的Bean，它的声明类型与实际类型不一定是同一类型
     *
     * @return Bean的声明类型
     */
    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    /**
     * 获取Bean的实例
     *
     * @return Bean的实例
     */
    @Nullable
    public Object getInstance() {
        return this.instance;
    }

    /**
     * 获取Bean实例，并确保实例不为null，如果实例为null，则抛出异常
     *
     * @return Bean实例
     */
    public Object getRequiredInstance() {
        // 检查当前对象的实例 instance 是否为 null
        if (this.instance == null) {
            throw new BeanCreationException(String.format("Instance of bean with name '%s' and type '%s' is not instantiated during current stage.", this.getName(), this.getBeanClass().getName()));
        }
        return this.instance;
    }

    /**
     * 设置Bean的实例，如果传入的实例为null，则抛出异常，如果传入的实例类型与当前对象的预期类型不兼容，则抛出异常
     *
     * @param instance Bean的实例
     */
    public void setInstance(Object instance) {
        // 检查传入的实例是否为 null
        Objects.requireNonNull(instance, "Bean instance is null.");
        // 检查传入的实例 instance 的类型是否与当前对象的预期类型兼容
        if (!this.beanClass.isAssignableFrom(instance.getClass())) {
            throw new BeanCreationException(String.format("Instance '%s' of Bean '%s' is not the expected type: %s", instance, instance.getClass().getName(), this.beanClass.getName()));
        }
        this.instance = instance;
    }

    /**
     * 检查是否标识了@Primary注解
     *
     * @return 如果标识了@Primary注解，则返回true；否则返回false
     */
    public boolean isPrimary() {
        return this.primary;
    }

    /**
     * 返回BeanDefinition对象的字符串表示形式
     *
     * @return BeanDefinition对象的字符串表示形式
     */
    @Override
    public String toString() {
        return "BeanDefinition [name=" + name +
                ", beanClass=" + beanClass.getName() +
                ", factory=" + getCreateDetail() +
                ", init-method=" + (initMethod == null ? "null" : initMethod.getName()) +
                ", destroy-method=" + (destroyMethod == null ? "null" : destroyMethod.getName()) +
                ", primary=" + primary +
                ", instance=" + instance + "]";
    }

    /**
     * 获取创建细节信息，描述对象是如何创建的
     *
     * @return 创建信息
     */
    private String getCreateDetail() {
        // 检查当前对象的工厂方法 factoryMethod 是否为非空
        if (this.factoryMethod != null) {
            // 通过反射获取工厂方法的参数类型数组，并使用流操作将参数类型转换为简单名称（即去除包名）
            String params = String.join(", ", Arrays.stream(this.factoryMethod.getParameterTypes()).map(t -> t.getSimpleName()).toArray(String[]::new));
            // 工厂方法声明类的简单名称.工厂方法名称(参数列表字符串)
            return this.factoryMethod.getDeclaringClass().getSimpleName() + "." + this.factoryMethod.getName() + "(" + params + ")";
        }
        return null;
    }

    /**
     * 比较当前BeanDefinition对象与传入的BeanDefinition对象的顺序
     *
     * @param def the object to be compared.
     * @return 当前BeanDefinition对象的order属性与传入BeanDefinition对象的order属性的差值，如果order属性相同，则根据名称（name属性）进行比较的结果
     */
    @Override
    public int compareTo(BeanDefinition def) {
        // 使用 Integer.compare() 方法比较当前 BeanDefinition 对象的 order 属性和传入的 BeanDefinition 对象 def 的 order 属性
        int cmp = Integer.compare(this.order, def.order);
        // 如果上述比较结果 cmp 不等于 0，则表示两个 BeanDefinition 对象的 order 属性不同
        if (cmp != 0) {
            return cmp;
        }
        // 使用 String.compareTo() 方法比较当前 BeanDefinition 对象的 name 属性和传入的 BeanDefinition 对象 def 的 name 属性
        // 在Java中，类的成员访问权限是基于类的边界，而不是基于方法。这意味着在同一个类中，无论成员是公共的、受保护的还是私有的，都可以直接访问它们
        return this.name.compareTo(def.name);
    }
}
