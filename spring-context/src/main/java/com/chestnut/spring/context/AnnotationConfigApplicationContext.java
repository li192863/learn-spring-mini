package com.chestnut.spring.context;

import com.chestnut.spring.annotation.*;
import com.chestnut.spring.exception.*;
import com.chestnut.spring.io.PropertyResolver;
import com.chestnut.spring.io.ResourceResolver;
import com.chestnut.spring.utils.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 注解配置应用程序上下文类
 * 它用于创建和管理Java对象（Bean）的实例，并负责解析属性配置、处理Bean后置处理器以及检测循环依赖。
 * <p>
 * 该类提供了一个基于Java注解的配置环境，让开发人员可以使用注解来定义和配置Bean，
 * 而不必依赖于XML配置文件。它通过扫描指定的包或类路径，自动发现和注册带有特定注解的Bean定义。
 * <p>
 * 使用该类可以实现基于注解的轻量级依赖注入(DI)和控制反转(IOC)，使得开发更加简洁和灵活。
 * <p>
 * 请注意，该类是一个上下文类，因此它会负责整个Bean的生命周期管理，包括Bean的初始化、依赖注入、销毁等。
 * 可以在应用程序中使用该上下文类来获取所需的Bean实例，从而促进了组件的解耦和可重用性。
 *
 * @author: Chestnut
 * @since: 2023-07-14
 **/
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {
    /**
     * 日志记录器
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 属性解析器，用于解析属性配置
     */
    protected final PropertyResolver propertyResolver;
    /**
     * 字符串到Bean定义的映射表
     * 用于存储通过注解配置的所有Bean定义，键为Bean的名称，值为对应的BeanDefinition对象
     */
    protected final Map<String, BeanDefinition> beans;
    /**
     * 后处理器列表，后处理器用于替换Bean
     * 在Bean创建过程中，会调用这些后处理器对Bean进行额外的处理，以满足特定需求
     */
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    /**
     * 跟踪当前正在创建的所有Bean的名称，以检测循环依赖
     * 当一个Bean正在创建时，它的名称会被添加到这个集合中。
     * 如果在创建Bean的过程中发现同一个Bean正在被递归创建，就表示存在循环依赖，将抛出异常。
     */
    private Set<String> creatingBeanNames = new HashSet<>();

    /**
     * 构造一个AnnotationConfigApplicationContext实例
     * <p>
     * 该构造函数用于创建AnnotationConfigApplicationContext的实例，负责解析配置类，并自动创建和管理Java对象（Bean）的实例。
     * 它将配置类中的Bean定义通过扫描解析，自动发现和注册带有特定注解的Bean，实现了基于注解的轻量级依赖注入和控制反转。
     * <p>
     * 该构造函数的执行步骤如下：
     * 1. 将传入的属性解析器赋值给当前对象的 propertyResolver 成员变量。
     * 2. 扫描配置类 configClass 中的所有 Bean 类的类名，获取需要创建的Bean类。
     * 3. 创建 Bean 的定义（BeanDefinition）并存储在 beans 成员变量中，用于后续的Bean创建和管理。
     * 4. 创建所有标注了 @Configuration 注解的 Bean，并将其创建为早期单例。
     * 5. 创建所有标注了 BeanPostProcessor 接口的 Bean，并添加到 beanPostProcessors 成员变量中。
     * 6. 创建其他普通 Bean，并通过字段和set方法注入依赖。
     * 7. 调用所有 Bean 的初始化方法（如果有的话）。
     * 8. 使用日志记录器输出初始化的 Bean 的信息（仅在debug模式下）。
     * <p>
     *请注意，该构造函数将初始化和管理整个应用程序的Bean生命周期，包括Bean的创建、初始化、依赖注入和销毁等过程。
     *
     * @param configClass      配置类，通常是带有@Configuration注解的类，其中包含了对Bean的定义和配置
     * @param propertyResolver 属性解析器，用于解析属性配置，可以用于配置Bean的属性值
     */
    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        // 将传入的属性解析器赋值给当前对象的 propertyResolver 成员变量
        this.propertyResolver = propertyResolver;

        // 扫描配置类 configClass 中的所有 Bean 类的类名
        final Set<String> beanClassNames = scanForClassNames(configClass);

        // 此时所有 要加载的类 均已知

        // 创建所有 Bean 的定义
        // 对于 @Component 定义的Bean，名称为注解指定的 value 或 小驼峰(类名)，声明类型为Class本身
        // 对于 @Bean 定义的Bean，名称为注解指定的 value 或 方法名，声明类型为@Bean方法签名中的返回值类型
        this.beans = createBeanDefinitions(beanClassNames);

        // 此时所有 Bean 均被定义

        // 创建 @Configuration 类型的 Bean，先创建已保证后续 @Bean注解的Bean 的创建
        this.beans.values().stream()
                // 过滤出具有 @Configuration 注解的 Bean 定义
                .filter(this::isConfigurationDefinition)
                .sorted()
                .map(def -> {
                    // 根据定义创建早期单例（不进行字段和方法级别的注入）
                    createBeanAsEarlySingleton(def);
                    return def.getName();
                }).collect(Collectors.toList());

        // 创建 BeanPostProcessor 类型的Bean，先创建已保证 Bean 可被后处理（也就是被替换）
        List<BeanPostProcessor> processors = this.beans.values().stream()
                // 过滤出为 BeanPostProcessor 的 Bean 定义
                .filter(this::isBeanPostProcessorDefinition)
                .sorted()
                // 根据定义创建早期单例（不进行字段和方法级别的注入）
                .map(def -> (BeanPostProcessor) createBeanAsEarlySingleton(def))
                .toList();
        this.beanPostProcessors.addAll(processors);

        // 创建其他普通 Bean
        createNormalBeans();

        // 此时所有 Bean 均被创建，且各个 Bean 的 instance 均已被设置，且已被 BeanPostProcessor 处理

        // 通过字段和set方法注入依赖
        this.beans.values().forEach(this::injectBean);

        // 调用初始化方法
        this.beans.values().forEach(this::initBean);

        // 使用日志记录器输出初始化的Bean
        if (logger.isDebugEnabled()) {
            this.beans.values().stream().sorted().forEach(def -> logger.debug("bean initialized: {}", def));
        }
    }

    /**
     * 判断容器中是否包含指定名称的Bean
     *
     * @param name 要检查的Bean名称
     * @return 如果容器中包含指定名称的Bean，则返回true；否则返回false
     */
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    /**
     * 根据指定类型获取所有符合条件的Bean定义列表，如果未找到，则返回空列表
     *
     * @param type 要获取的Bean定义类型
     * @return 指定类型的Bean定义列表
     */
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
                // 检查type是否为def.getBeanClass()（即def的声明类型）的父类，若可以则保留
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 根据指定的名称获取对应的Bean定义，如果未找到，则返回null
     *
     * @param name 要获取的Bean定义的名称
     * @return 指定名称对应的Bean定义
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }

    /**
     * 根据指定的名称和类型获取对应的Bean定义，如果未找到，则返回null，如果找到但类型不匹配，则抛出异常
     *
     * @param name         要获取的Bean的定义名称
     * @param requiredType 要获取的Bean定义类型
     * @return 指定名称和类型对应的Bean定义
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        // 如果找到的 Bean 定义为 null，表示指定名称的 Bean 不存在，直接返回 null
        if (def == null) {
            return null;
        }
        // 检查requiredType是否为def.getBeanClass()（即def的声明类型）的父类
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.", requiredType.getName(), name, def.getBeanClass().getName()));
        }
        return def;
    }

    /**
     * 根据指定类型获取对应的Bean定义，如果未找到，则返回null
     * 如果存在多个，则优先选择标记了@Primary注解的定义；
     * 如果存在多个@Primary注解，或者没有@Primary注解但存在多个定义，则抛出NoUniqueBeanDefinitionException异常
     *
     * @param type 要获取的Bean定义类型
     * @return 指定类型对应的Bean定义
     */
    @Nullable
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> defs = findBeanDefinitions(type);
        // 如果列表 defs 是空的，表示不存在符合条件的 Bean 定义，直接返回 null
        if (defs.isEmpty()) {
            return null;
        }
        // 如果列表 defs 的大小为 1，表示只存在一个符合条件的 Bean 定义，直接返回该 Bean 定义
        if (defs.size() == 1) {
            return defs.get(0);
        }
        // 流式操作筛选出标记了 @Primary 注解的 Bean 定义，将它们存储在列表 primaryDefs 中
        List<BeanDefinition> primaryDefs = defs.stream()
                .filter(BeanDefinition::isPrimary)
                .toList();
        // 只存在一个标记了 @Primary 注解的 Bean 定义
        if (primaryDefs.size() == 1) {
            return primaryDefs.get(0);
        }
        if (primaryDefs.isEmpty()) {
            // 没有标记了 @Primary 注解的 Bean 定义
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else {
            // 存在多个标记了 @Primary 注解的 Bean 定义
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }

    /**
     * 根据指定类型获取所有符合条件的Bean实例列表，如果未找到，则返回空列表
     *
     * @param requiredType 要获取的Bean类型
     * @param <T>          Bean的类型
     * @return 指定类型的Bean实例列表
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> List<T> findBeans(Class<T> requiredType) {
        return findBeanDefinitions(requiredType).stream()
                .map(def -> (T) def.getRequiredInstance())
                .collect(Collectors.toList());
    }

    /**
     * 根据指定的名称获取对应的Bean实例，如果未找到，则返回null
     *
     * @param name 要获取的Bean的名称
     * @param <T>  Bean的类型
     * @return 指定名称对应的Bean实例
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 根据名称和指定类型获取对应的Bean实例，如果未找到，则返回null
     *
     * @param name         要获取的Bean的名称
     * @param requiredType 要获取的Bean类型
     * @param <T>          Bean的类型
     * @return 指定名称和类型对应的Bean实例
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 根据指定类型查找Bean实例，如果未找到，则返回null
     *
     * @param requiredType 要获取的Bean类型
     * @param <T>          Bean的类型
     * @return 指定类型对应的Bean实例
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 根据指定类型获取所有符合条件的Bean实例列表，如果未找到，则返回空列表
     *
     * @param requiredType 要获取的Bean类型
     * @param <T>          Bean的类型
     * @return 指定类型的Bean实例列表
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        return findBeanDefinitions(requiredType).stream()
                .map(def -> (T) def.getRequiredInstance())
                .collect(Collectors.toList());
    }

    /**
     * 根据指定的名称获取对应的Bean实例，如果未找到，则抛出异常
     *
     * @param name 要获取的Bean的名称
     * @param <T>  Bean的类型
     * @return 指定名称对应的Bean实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 根据指定的名称和类型获取对应的Bean实例，如果未找到，则抛出异常
     *
     * @param name         要获取的Bean的名称
     * @param requiredType 要获取的Bean类型
     * @param <T>          Bean的类型
     * @return 指定名称和类型对应的Bean实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s' and type '%s'.", name, requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 根据指定类型获取对应的Bean实例，如果未找到，则抛出异常
     *
     * @param requiredType 要获取的Bean类型
     * @param <T>          Bean的类型
     * @return 指定类型对应的Bean实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 执行组件扫描并返回类名集合
     *
     * @param configClass 配置类
     * @return 类名集合
     */
    protected Set<String> scanForClassNames(Class<?> configClass) {
        // 获取要扫描的package名称
        // 查找配置类 configClass 上的 ComponentScan 注解
        ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        // 根据 ComponentScan 注解的值获取要扫描的包名，如果 ComponentScan 注解不存在或其值为空，则默认扫描配置类所在的包
        final String[] scanPackages = scan == null || scan.value().length == 0 ? new String[]{configClass.getPackage().getName()} : scan.value();
        // 使用日志记录器输出组件扫描的包名信息
        logger.atInfo().log("component scan in packages: {}", Arrays.toString(scanPackages));

        Set<String> classNameSet = new HashSet<>();
        // 扫描package
        // 遍历扫描包的数组，每次处理一个包
        for (String pkg : scanPackages) {
            // 使用日志记录器输出当前正在扫描的包名
            logger.atDebug().log("scan package: {}", pkg);
            // 创建资源解析器
            ResourceResolver rr = new ResourceResolver(pkg);
            // 扫描包中的资源，并返回符合条件的类名集合
            List<String> classList = rr.scan(res -> {
                String name = res.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });
            // 如果调试日志可用，则遍历类名列表，并使用日志记录器输出每个类名
            if (logger.isDebugEnabled()) {
                classList.forEach((className) -> logger.debug("class found by component scan: {}", className));
            }
            // 将扫描到的类名集合添加到总的类名集合中
            classNameSet.addAll(classList);
        }

        // 查找@Import(Xyz.class)
        // 获取配置类上的 Import 注解，Import 注解用于指定要导入的其他类
        Import importConfig = configClass.getAnnotation(Import.class);
        // 如果 Import 注解不存在，则直接返回当前的类名集合
        if (importConfig == null) {
            return classNameSet;
        }
        // 遍历 Import 注解中指定的要导入的其他类
        for (Class<?> importConfigClass : importConfig.value()) {
            String importClassName = importConfigClass.getName();
            if (classNameSet.contains(importClassName)) {
                // 如果类名集合中已经包含当前要导入的类名，则输出警告信息
                logger.warn("ignore import: " + importClassName + " for it is already been scanned.");
            } else {
                // 将当前要导入的类名添加到类名集合中
                logger.debug("class found by import: {}", importClassName);
                classNameSet.add(importClassName);
            }
        }
        return classNameSet;
    }

    /**
     * 根据扫描的ClassName创建Bean定义
     * 对于 @Component 定义的Bean，名称为注解指定的 value 或 小驼峰(类名)，声明类型为Class本身
     * 对于 @Bean 定义的Bean，名称为注解指定的 value 或 方法名，声明类型为@Bean方法签名中的返回值类型
     *
     * @param classNameSet 扫描到的类名集合
     * @return Bean定义集合
     */
    private Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNameSet) {
        Map<String, BeanDefinition> defs = new HashMap<>();
        // 遍历扫描到的类名集合，每次处理一个类名
        for (String className : classNameSet) {
            // 获取Class对象
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }
            // 检查类是否为注解、枚举、接口或记录类型，如果是，则跳过
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
                continue;
            }

            // 是否标注@Component
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            // 如果注解不存在，则跳过当前类的处理
            if (component == null) {
                continue;
            }
            logger.atDebug().log("found component: {}", clazz.getName());
            // 获取当前类的修饰符
            int mod = clazz.getModifiers();
            // 检查当前类是否为抽象类
            if (Modifier.isAbstract(mod)) {
                throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
            }
            // 检查当前类是否为私有类，嵌套类可以是私有的
            if (Modifier.isPrivate(mod)) {
                throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
            }

            // 使用 ClassUtils.getBeanName() 方法根据类获取 Bean 的名称
            String beanName = ClassUtils.getBeanName(clazz);
            // 创建一个新的 BeanDefinition 对象
            BeanDefinition def = new BeanDefinition(beanName,
                    clazz,
                    // 构造函数，包括私有/默认构造函数
                    getSuitableConstructor(clazz),
                    getOrder(clazz),
                    clazz.isAnnotationPresent(Primary.class),
                    null,
                    null,
                    // 类中找带有特定注解的方法（此处为找初始方法），若没有则返回null
                    ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                    // 类中找带有特定注解的方法（此处为找销毁方法），若没有则返回null
                    ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
            addBeanDefinitions(defs, def);
            logger.atDebug().log("define bean: {}", def);

            // 是否标注@Configuration
            Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
            if (configuration != null) {
                // 如果当前类是配置类，则调用 scanFactoryMethods() 方法扫描工厂方法，并将其添加到 Bean 定义集合中
                scanFactoryMethods(beanName, clazz, defs);
            }
        }
        return defs;
    }

    /**
     * 创建一个Bean，但不进行字段和方法级别的注入。
     * 如果创建的Bean不是Configuration或BeanPostProcessor，则在*构造方法中注入的依赖Bean*会自动创建。
     *
     * @param def Bean的定义
     * @return Bean的实例
     */
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        logger.atDebug().log("Try create bean '{}' as early singleton: {}", def.getName(), def.getBeanClass().getName());
        // 检测循环依赖，并将当前正在创建的Bean放入集合中
        // add方法若creatingBeanNames存在该元素则返回False，即抛出异常
        if (!this.creatingBeanNames.add(def.getName())) {
            throw new UnsatisfiedDependencyException(String.format("Circular dependency detected when create bean '%s'", def.getName()));
        }

        // 根据 BeanDefinition 中的信息确定创建 Bean 的方式
        Executable createFn = null;
        if (def.getFactoryName() == null) {
            // 构造方法构造
            createFn = def.getConstructor();
        } else {
            // 工厂方法构造
            createFn = def.getFactoryMethod();
        }

        // 获取创建 Bean 所需的参数
        // 如果创建的Bean不是Configuration或BeanPostProcessor，则在*构造方法中注入的依赖Bean*会自动创建。
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] parametersAnnos = createFn.getParameterAnnotations();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            // 参数
            final Parameter param = parameters[i];
            // 注解列表
            final Annotation[] paramAnnos = parametersAnnos[i];
            // 检查是否存在 @Value 注解
            final Value value = ClassUtils.getAnnotation(paramAnnos, Value.class);
            // 检查是否存在 @Autowired 注解
            final Autowired autowired = ClassUtils.getAnnotation(paramAnnos, Autowired.class);

            // 参数校验
            final boolean isConfiguration = isConfigurationDefinition(def);
            // @Configuration类型的Bean是工厂，不允许使用@Autowired创建
            if (isConfiguration && autowired != null) {
                throw new BeanCreationException(String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            // 参数需要 @Value 或 @Autowired 两者之一
            if (value != null && autowired != null) {
                throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            // 创建Bean时需要 @Value 或 @Autowired 两者之一
            if (value == null && autowired == null) {
                throw new BeanCreationException(String.format("Must specify @Autowired or @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }

            // 参数类型
            final Class<?> type = param.getType();
            // 参数设置为查询的 @Value
            if (value != null) {
                // 获取指定属性的值，并将其转换为当前参数的类型
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            }
            // 参数是@Autowired，查找依赖的BeanDefinition
            if (autowired != null) {
                String name = autowired.name();
                boolean required = autowired.value();
                // 依赖的BeanDefinition
                BeanDefinition dependsOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                // 检测required == true，注意 Bean 的定义应该存在，若不存在则报错
                if (required && dependsOnDef == null) {
                    throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(), def.getName(), def.getBeanClass().getName()));
                }
                if (dependsOnDef != null) {
                    // 获取依赖Bean实例
                    Object autowiredBeanInstance = dependsOnDef.getInstance();
                    // 当前依赖Bean尚未初始化，递归调用初始化该依赖Bean
                    if (autowiredBeanInstance == null/* && !isConfiguration */) {
                        // 当前依赖Bean尚未初始化，递归调用初始化该依赖Bean
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependsOnDef);
                    }
                    args[i] = autowiredBeanInstance;
                } else {
                    args[i] = null;
                }
            }
        }

        // 创建Bean实例
        Object instance = null;
        if (def.getFactoryName() == null) {
            // 用构造方法创建
            try {
                instance = def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        } else {
            // 用@Bean方法创建
            // configInstance为配置类实例，此时配置类已完成创建，故正常情况下getBean不会报错
            Object configInstance = getBean(def.getFactoryName());
            try {
                instance = def.getFactoryMethod().invoke(configInstance, args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        }
        // 设置 Bean 定义的实例
        def.setInstance(instance);

        // 调用 BeanPostProcessor 处理Bean（也就是替换Bean）
        // 一个Bean如果被Proxy替换，则依赖它的Bean应注入Proxy
        // BeanDefinition 中的 instance 将被替换为 Proxy
        // 按beanPostProcessors的顺序依次进行替换
        for (BeanPostProcessor processor : beanPostProcessors) {
            // Bean 定义的实例已经构建完成（但还未注入依赖）
            Object processed = processor.postProcessBeforeInitialization(def.getInstance(), def.getName());
            if (processed == null) {
                throw new BeanCreationException(String.format("PostBeanProcessor returns null when process bean '%s' by %s", def.getName(), processor));
            }
            // 如果一个BeanPostProcessor替换了原始Bean，则更新Bean的引用
            if (def.getInstance() != processed) {
                logger.atDebug().log("Bean '{}' was replaced by post processor {}.", def.getName(), processor.getClass().getName());
                def.setInstance(processed);
            }
        }
        return def.getInstance();
    }

    /**
     * 创建普通的Bean。确保所有普通Bean都被创建
     */
    private void createNormalBeans() {
        // 获取所有未创建实例的 BeanDefinition 列表
        List<BeanDefinition> defs = this.beans.values().stream()
                // 过滤出所有未创建实例的 BeanDefinition
                .filter(def -> def.getInstance() == null)
                // 排序
                .sorted()
                .toList();
        // 创建普通的Bean
        defs.forEach(def -> {
            // 如果Bean未被创建（可能在其他Bean的构造方法注入前被创建）
            if (def.getInstance() == null) {
                // 创建Bean
                createBeanAsEarlySingleton(def);
            }
        });
    }

    /**
     * 注入依赖，但不调用init方法
     *
     * @param def 要进行属性注入的 Bean定义
     */
    private void injectBean(BeanDefinition def) {
        // 获取Bean实例，或被代理的原始实例
        // 一个Bean如果被Proxy替换，如果要注入依赖，则应该注入到原始对象
        // getProxiedInstance 用于获取原始的未经过代理的 Bean 实例
        Object beanInstance = getProxiedInstance(def);
        try {
            // 为指定的Bean实例注入属性值，会传入def的声明类型
            injectProperties(def, def.getBeanClass(), beanInstance);
        } catch (ReflectiveOperationException e) {
            throw new BeanCreationException(e);
        }
    }

    /**
     * 获取原始 Bean实例 或未经过代理的 Bean 实例。
     * 在依赖注入过程中，如果使用了代理（例如AOP），Bean可能会被代理修改。该方法用于获取原始的未经过代理的Bean实例。
     * 该类处理代理改变了原始Bean但又希望注入到原始Bean的情况
     *
     * @param def 获取 Bean 实例的 BeanDefinition 对象
     * @return Bean实例，或被代理的原始实例
     */
    private Object getProxiedInstance(BeanDefinition def) {
        // BeanDefinition 中的 instance 为 原始对象 或 Proxy
        Object beanInstance = def.getInstance();
        // 如果Proxy改变了原始Bean，又希望注入到原始Bean，则由BeanPostProcessor指定原始Bean
        List<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(this.beanPostProcessors);
        Collections.reverse(reversedBeanPostProcessors);
        // 按beanPostProcessors的逆序依次进行还原
        for (BeanPostProcessor beanPostProcessor : reversedBeanPostProcessors) {
            Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(beanInstance, def.getName());
            if (restoredInstance != beanInstance) {
                logger.atDebug().log("BeanPostProcessor {} specified injection from {} to {}.", beanPostProcessor.getClass().getSimpleName(), beanInstance.getClass().getSimpleName(), restoredInstance.getClass().getSimpleName());
                beanInstance = restoredInstance;
            }
        }
        return beanInstance;
    }

    /**
     * 调用初始化方法
     *
     * @param def 要进行初始化的 Bean定义
     */
    private void initBean(BeanDefinition def) {
        callMethod(def.getInstance(), def.getInitMethod(), def.getInitMethodName());
    }

    /**
     * 调用对象的初始化方法或指定的命名方法
     *
     * @param beanInstance 调用方法的对象实例
     * @param method       要调用的初始化方法
     * @param namedMethod  要调用的指定命名方法的名称
     */
    private void callMethod(Object beanInstance, Method method, String namedMethod) {
        if (method != null) {
            // Bean为构造方法 @Component 构建，namedMethod为空
            try {
                method.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        } else if (namedMethod != null) {
            // Bean为工厂方法 @Bean 构建，method为空
            // 查找initMethod/destroyMethod="xyz"，注意是在实际类型中查找
            Method named = ClassUtils.getNamedMethod(beanInstance.getClass(), namedMethod);
            named.setAccessible(true);
            try {
                named.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        }
    }

    /**
     * 关闭并执行所有bean的destroy方法
     * 在关闭容器之前，会依次执行所有bean的destroy方法，以释放资源或执行清理操作。
     */
    @Override
    public void close() {
        logger.info("Closing {}...", this.getClass().getName());
        // 遍历所有BeanDefinition，执行每个bean的destroy方法
        this.beans.values().forEach(def -> {
            final Object beanInstance = getProxiedInstance(def);
            callMethod(beanInstance, def.getDestroyMethod(), def.getDestroyMethodName());
        });
        // 清空BeanDefinition集合
        this.beans.clear();
        // 将ApplicationContextUtils中的ApplicationContext设置为null，表示容器已关闭
        logger.info("{} closed.", this.getClass().getName());
        ApplicationContextUtils.setApplicationContext(null);
    }

    /**
     * 为指定的Bean实例注入属性值
     *
     * @param def   Bean的定义
     * @param clazz Bean的声明类型，或Bean的声明类型的父类
     * @param bean  要进行属性注入的Bean实例，可能为原始未经代理的Bean实例
     * @throws ReflectiveOperationException 如果在注入属性值时发生反射操作异常，则抛出此异常
     */
    private void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws ReflectiveOperationException {
        // 在当前类查找 Field 和 Method 并注入
        for (Field f : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, f);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, m);
        }
        // 在父类查找Field和Method并注入
        // If this Class object represents either the Object class, an interface, a primitive type, or void, then null is returned
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            injectProperties(def, superClazz, bean);
        }
    }

    /**
     * 尝试将属性或方法注入到Bean实例中
     *
     * @param def   Bean的定义
     * @param clazz Bean的声明类型，或Bean的声明类型的父类
     * @param bean  要进行属性注入的Bean实例，可能为原始未经代理的Bean实例
     * @param acc   可访问对象（属性或方法）
     * @throws ReflectiveOperationException 如果发生依赖不满足的异常，则抛出此异常
     */
    private void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws ReflectiveOperationException {
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        // 无需注入
        if (value == null && autowired == null) {
            return;
        }
        Field field = null;
        Method method = null;
        if (acc instanceof Field f) {
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if (acc instanceof Method m) {
            checkFieldOrMethod(m);
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(String.format("Cannot inject a non-setter method %s for bean '%s': %s", m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }
        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessibleType = field != null ? field.getType() : method.getParameterTypes()[0];
        // 参数需要 @Value 或 @Autowired 两者之一
        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s", clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }
        // 参数设置为查询的 @Value
        if (value != null) {
            Object propValue = this.propertyResolver.getRequiredProperty(value.value(), accessibleType);
            if (field != null) {
                logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, propValue);
                field.set(bean, propValue);
            }
            if (method != null) {
                logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, propValue);
                method.invoke(bean, propValue);
            }
        }
        // 参数是@Autowired，查找依赖的BeanDefinition
        if (autowired != null) {
            String name = autowired.name();
            boolean required = autowired.value();
            // 依赖的Bean
            Object depends = name.isEmpty() ? findBean(accessibleType) : findBean(name, accessibleType);
            // 检测required == true，注意 Bean 的应该存在，若不存在则报错
            if (required && depends == null) {
                throw new UnsatisfiedDependencyException(String.format("Dependency bean not found when inject %s.%s for bean '%s': %s", clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
            }
            // 依赖的Bean不为空，需要设置
            if (depends != null) {
                if (field != null) {
                    logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, depends);
                    field.set(bean, depends);
                }
                if (method != null) {
                    logger.atDebug().log("Mield injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, depends);
                    method.invoke(bean, depends);
                }
            }
        }
    }

    /**
     * 检查成员（字段或方法）是否可以注入到Bean实例中
     *
     * @param m 要检查的成员（属性或方法）
     */
    private void checkFieldOrMethod(Member m) {
        // 获取属性或方法的修饰符
        int mod = m.getModifiers();
        // 静态属性/方法，不能注入
        if (Modifier.isStatic(mod)) {
            throw new BeanDefinitionException("Cannot inject static field: " + m);
        }
        // 常量属性/方法
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field field) {
                throw new BeanDefinitionException("Cannot inject final field: " + field);
            }
            if (m instanceof Method) {
                logger.warn("Inject final method should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException.");
            }
        }
    }

    /**
     * 获取适合的构造函数，公共构造函数作为首选，非公共构造函数作为后备
     *
     * @param clazz 类型
     * @return 构造函数对象
     */
    private Constructor<?> getSuitableConstructor(Class<?> clazz) {
        // 获取类的公共构造函数数组
        Constructor<?>[] cons = clazz.getConstructors();
        // 获取类的公共构造函数数组
        if (cons.length == 0) {
            // 获取类的所有构造函数数组，包括私有构造函数/默认构造函数
            // This method returns an array of length 0 if this Class object represents an interface, a primitive type, an array class, or void.
            cons = clazz.getDeclaredConstructors();
            // 如果类的构造函数数组的长度不等于1，说明存在多个构造函数
            if (cons.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        // 如果类的构造函数数组的长度不等于 1，说明存在多个公共构造函数
        if (cons.length != 1) {
            throw new BeanDefinitionException("More than one public constructor found in class " + clazz.getName() + ".");
        }
        return cons[0];
    }

    /**
     * 扫描标注了@Bean注解的工厂方法
     *
     * @param factoryBeanName 工厂Bean名称
     * @param clazz           工厂Bean的Class
     * @param defs            Bean定义集合
     */
    private void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        // 遍历工厂 Bean 的类中声明的所有方法，对每个方法进行处理
        for (Method method : clazz.getDeclaredMethods()) {
            // 使用 getAnnotation(Bean.class) 方法获取方法上的 @Bean 注解
            Bean bean = method.getAnnotation(Bean.class);
            // 如果方法上没有 @Bean 注解，则跳过该方法的处理
            if (bean == null) {
                continue;
            }
            // 获取方法的修饰符
            int mod = method.getModifiers();
            // 检查方法是否为抽象方法
            if (Modifier.isAbstract(mod)) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
            }
            // 检查方法是否为 final 方法
            if (Modifier.isFinal(mod)) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
            }
            // 检查方法是否为私有方法
            if (Modifier.isPrivate(mod)) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
            }

            // 获取方法的返回类型
            Class<?> beanClass = method.getReturnType();
            // 检查方法的返回类型是否为原始类型
            if (beanClass.isPrimitive()) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
            }
            // 检查方法的返回类型是否为 void
            if (beanClass == void.class || beanClass == Void.class) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
            }

            // 使用 ClassUtils.getBeanName() 方法根据方法获取 Bean 的名称
            String beanName = ClassUtils.getBeanName(method);
            // 创建一个新的 BeanDefinition 对象
            BeanDefinition def = new BeanDefinition(beanName,
                    beanClass,
                    // 通常为 "XyzConfiguration"
                    factoryBeanName,
                    // 通常为 @Bean 标注的一个方法
                    method,
                    getOrder(method),
                    method.isAnnotationPresent(Primary.class),
                    // 注解中指定找初始方法名称，若没有则返回null
                    bean.initMethod().isEmpty() ? null : bean.initMethod(),
                    // 注解中指定找销毁方法名称，若没有则返回null
                    bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                    null,
                    null);
            addBeanDefinitions(defs, def);
            logger.atDebug().log("define bean: {}", def);
        }
    }

    /**
     * 检查并添加Bean定义，可以确保每个 Bean 的名称在定义集合中是唯一的，避免命名冲突
     *
     * @param defs Bean定义集合
     * @param def  要添加的Bean定义
     */
    private void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def) {
        // 如果已经存在相同名称的 Bean 定义，则 put() 方法返回之前与该名称关联的旧值，并将新值替换旧值。
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + def.getName());
        }
    }

    /**
     * 根据@Order注解获取类的排序值
     *
     * @param clazz 类型
     * @return 排序值
     */
    private int getOrder(Class<?> clazz) {
        // 使用 getAnnotation(Order.class) 方法获取类上的 @Order 注解
        Order order = clazz.getAnnotation(Order.class);
        // 如果 order 变量为 null，说明类上没有 @Order 注解，返回 Integer.MAX_VALUE 表示最高优先级
        // 如果 order 变量不为 null，调用 value() 方法获取 @Order 注解的排序值，并返回该值
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * 根据@Order注解获取方法的排序值
     *
     * @param method 方法
     * @return 排序值
     */
    private int getOrder(Method method) {
        // 使用 getAnnotation(Order.class) 方法获取方法上的 @Order 注解
        Order order = method.getAnnotation(Order.class);
        // 如果 order 变量为 null，说明方法上没有 @Order 注解，返回 Integer.MAX_VALUE 表示最高优先级
        // 如果 order 变量不为 null，调用 value() 方法获取 @Order 注解的排序值，并返回该值
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * 检查是否是配置类的定义
     *
     * @param def Bean定义
     * @return 如果是配置类的定义，则返回true；否则返回false
     */
    private boolean isConfigurationDefinition(BeanDefinition def) {
        // 检查def的声明类型中是否包含 @Configuration 注解
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    /**
     * 检查是否是Bean后处理类的定义
     *
     * @param def Bean定义
     * @return 如果是配置类的定义，则返回true；否则返回false
     */
    private boolean isBeanPostProcessorDefinition(BeanDefinition def) {
        // 检查def的声明类型中是否为 BeanPostProcessor 子类
        return BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
    }
}
