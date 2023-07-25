package com.chestnut.spring.io;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * 属性解析器，用于解析属性配置
 *
 * @author: Chestnut
 * @since: 2023-07-13
 **/
public class PropertyResolver {
    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 属性
     */
    private Map<String, String> properties = new HashMap<>();
    /**
     * 类型解析器
     */
    private Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    /**
     * 创建一个PropertyResolver实例
     * @param props 属性
     */
    public PropertyResolver(Properties props) {
        // 将系统环境变量添加到属性映射中
        this.properties.putAll(System.getenv());
        // 遍历给定属性的所有键值对，并添加到属性映射中
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
        // 如果日志级别为调试模式，则打印所有属性的键值对
        if (logger.isDebugEnabled()) {
            List<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                logger.debug("PropertyResolver: {} = {}", key, this.properties.get(key));
            }
        }
        // 注册转换器
        converters.put(String.class, s -> s);

        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));

        converters.put(byte.class, s -> Byte.parseByte(s));
        converters.put(Byte.class, s -> Byte.valueOf(s));

        converters.put(short.class, s -> Short.parseShort(s));
        converters.put(Short.class, s -> Short.valueOf(s));

        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));

        converters.put(long.class, s -> Long.parseLong(s));
        converters.put(Long.class, s -> Long.valueOf(s));

        converters.put(float.class, s -> Float.parseFloat(s));
        converters.put(Float.class, s -> Float.valueOf(s));

        converters.put(double.class, s -> Double.parseDouble(s));
        converters.put(Double.class, s -> Double.valueOf(s));

        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));
    }

    /**
     * 检查属性解析器是否包含指定的属性键
     *
     * @param key 属性键
     * @return 如果解析器包含指定的属性键，则返回true；否则返回false
     */
    public boolean containsProperty(String key) {
        return this.properties.containsKey(key);
    }

    /**
     * 获取指定属性的值的字符串
     *
     * @param key 属性键
     * @return 属性值字符串，如果属性不存在，则返回null
     */
    @Nullable
    public String getProperty(String key) {
        // 解析${abc.xyz:defaultValue}
        PropertyExpr keyExpr = parsePropertyExpr(key);
        if (keyExpr != null) {
            if (keyExpr.defaultValue() != null) {
                // 带默认值查询
                return getProperty(keyExpr.key(), keyExpr.defaultValue());
            } else {
                // 不带默认值查询
                return getRequiredProperty(keyExpr.key());
            }
        }
        // 普通key查询
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return null;
    }

    /**
     * 获取指定属性的值的字符串，如果属性不存在，则返回默认值字符串
     *
     * @param key          属性键
     * @param defaultValue 默认值字符串
     * @return 属性值字符串，或默认值字符串
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    /**
     * 获取指定属性的值，并将其转换为指定类型
     *
     * @param key        属性键
     * @param targetType 属性值的目标类型
     * @param <T>        属性值的目标类型
     * @return 转换后的属性值
     */
    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(targetType, value);
    }

    /**
     * 获取指定属性的值，并将其转换为指定类型，如果属性不存在，则返回默认值
     *
     * @param key          属性键
     * @param targetType   属性值的目标类型
     * @param defaultValue 默认值
     * @param <T>          属性值的目标类型
     * @return 转换后的属性值，或默认值
     */
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }

    /**
     * 获取指定属性的值，如果属性不存在，则抛出异常
     *
     * @param key 属性键
     * @return 属性值字符串
     */
    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    /**
     * 获取指定属性的值，并将其转换为指定类型，如果属性不存在，则抛出异常。
     *
     * @param key        属性键
     * @param targetType 属性值的目标类型
     * @param <T>        属性值的目标类型
     * @return 转换后的属性值
     */
    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    /**
     * 将属性值转换为指定类型的对象
     *
     * @param clazz 目标对象的类
     * @param value 属性值字符串
     * @param <T>   目标对象的类型
     * @return 转换后的对象
     */
    @SuppressWarnings("unchecked")
    private <T> T convert(Class<?> clazz, String value) {
        Function<String, Object> fn = this.converters.get(clazz);
        if (fn == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }

    /**
     * 解析属性值
     *
     * @param value 属性值字符串
     * @return 解析后的属性值字符串
     */
    private String parseValue(String value) {
        PropertyExpr expr = parsePropertyExpr(value);
        // 非属性表达式
        if (expr == null) {
            return value;
        }
        if (expr.defaultValue() != null) {
            // 有默认值
            return getProperty(expr.key(), expr.defaultValue());
        } else {
            // 无默认值
            return getRequiredProperty(expr.key());
        }
    }

    /**
     * 解析后的属性值，${abc.xyz:defaultValue}
     *
     * @param key 属性键
     * @return 解析后的属性表达式，如果不是属性表达式，则返回null
     */
    private PropertyExpr parsePropertyExpr(String key) {
        // 解析${abc.xyz:defaultValue}
        if (!key.startsWith("${") || !key.endsWith("}")) {
            return null;
        }
        int n = key.indexOf(':');
        if (n == (-1)) {
            // 没有默认值
            String k = notEmpty(key.substring(2, key.length() - 1));
            return new PropertyExpr(k, null);
        } else {
            // 有默认值
            String k = notEmpty(key.substring(2, n));
            return new PropertyExpr(k, key.substring(n + 1, key.length() - 1));
        }
    }

    /**
     * 检查字符串是否为空，如果为空则抛出异常
     *
     * @param key 字符串
     * @return 非空字符串
     */
    private String notEmpty(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }
}
