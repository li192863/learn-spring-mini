package com.chestnut.spring.web.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON工具类
 *
 * @author: Chestnut
 * @since: 2023-07-22
 **/
public class JsonUtils {
    /**
     * 对象映射器
     */
    public static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    /**
     * 创建一个配置好的ObjectMapper实例
     *
     * @return 配置好的ObjectMapper实例
     */
    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        // 设置序列化时包含非空字段的属性（即使该属性的值为null）
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 禁用在反序列化时遇到未知属性时抛出异常
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 禁用在序列化时遇到空对象时抛出异常
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 禁用将日期序列化为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * 将Java对象转换成JSON字符串
     *
     * @param obj 要转换的Java对象
     * @return 转换后的JSON字符串
     */
    public static String writeJson(Object obj) {
        try {
            // 使用OBJECT_MAPPER的writeValueAsString方法将对象转换为JSON字符串
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 将Java对象转换成JSON字符串，并将结果写入到一个Writer中
     *
     * @param writer 要写入JSON字符串的Writer
     * @param obj    要转换的Java对象
     * @throws IOException 如果在写入过程中发生I/O错误
     */
    public static void writeJson(Writer writer, Object obj) throws IOException {
        try {
            // 使用OBJECT_MAPPER的writeValue方法将对象转换为JSON并写入到Writer中
            OBJECT_MAPPER.writeValue(writer, obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 将Java对象转换成JSON字符串，并将结果写入到一个输出流中
     *
     * @param output 要写入JSON字符串的输出流
     * @param obj    要转换的Java对象
     * @throws IOException 如果在写入过程中发生I/O错误
     */
    public static void writeJson(OutputStream output, Object obj) throws IOException {
        try {
            // 使用OBJECT_MAPPER的writeValue方法将对象转换为JSON并写入到输出流中
            OBJECT_MAPPER.writeValue(output, obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 将JSON字符串转换成Java对象
     *
     * @param str   要转换的JSON字符串
     * @param clazz 要转换成的目标Java类
     * @param <T>   目标Java类的类型
     * @return 转换后的Java对象
     */
    public static <T> T readJson(String str, Class<T> clazz) {
        try {
            // 使用OBJECT_MAPPER的readValue方法将JSON字符串转换为指定的Java对象
            return OBJECT_MAPPER.readValue(str, clazz);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 将JSON字符串转换成Java对象
     *
     * @param str 将JSON字符串转换成Java对象
     * @param ref 用于指定目标类型的TypeReference对象，能够在运行时获取泛型类型的具体信息
     * @param <T> 目标Java对象的类型
     * @return 转换后的Java对象
     */
    public static <T> T readJson(String str, TypeReference<T> ref) {
        try {
            // 使用OBJECT_MAPPER的readValue方法将JSON字符串转换为指定的Java对象
            return OBJECT_MAPPER.readValue(str, ref);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 从Reader中读取JSON字符串，并将其转换成Java对象
     *
     * @param reader 要读取JSON字符串的Reader
     * @param clazz  要转换成的目标Java类
     * @param <T>    目标Java类的类型
     * @return 转换后的Java对象
     */
    public static <T> T readJson(Reader reader, Class<T> clazz) {
        try {
            // 使用OBJECT_MAPPER的readValue方法从Reader中读取JSON字符串并转换为指定的Java对象
            return OBJECT_MAPPER.readValue(reader, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 从Reader中读取JSON字符串，并将其转换成Java对象
     *
     * @param reader 要读取JSON字符串的Reader
     * @param ref    用于指定目标类型的TypeReference对象，能够在运行时获取泛型类型的具体信息
     * @param <T>    目标Java对象的类型
     * @return 转换后的Java对象
     */
    public static <T> T readJson(Reader reader, TypeReference<T> ref) {
        try {
            // 使用OBJECT_MAPPER的readValue方法从Reader中读取JSON字符串并转换为指定的Java对象
            return OBJECT_MAPPER.readValue(reader, ref);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 从InputStream中读取JSON数据，并将其转换成Java对象
     *
     * @param input 要读取JSON数据的InputStream
     * @param clazz 要转换成的目标Java类
     * @param <T>   目标Java类的类型
     * @return 转换后的Java对象
     */
    public static <T> T readJson(InputStream input, Class<T> clazz) {
        try {
            // 使用OBJECT_MAPPER的readValue方法从InputStream中读取JSON数据并转换为指定的Java对象
            return OBJECT_MAPPER.readValue(input, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 从InputStream中读取JSON数据，并将其转换成Java对象
     *
     * @param input 要读取JSON数据的InputStream
     * @param ref   用于指定目标类型的TypeReference对象，能够在运行时获取泛型类型的具体信息
     * @param <T>   目标Java对象的类型
     * @return 转换后的Java对象
     */
    public static <T> T readJson(InputStream input, TypeReference<T> ref) {
        try {
            return OBJECT_MAPPER.readValue(input, ref);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 将JSON数据从字节数组转换成Java对象
     *
     * @param src   要转换的JSON数据字节数组
     * @param clazz 要转换成的目标Java类
     * @param <T>   目标Java类的类型
     * @return 转换后的Java对象
     */
    public static <T> T readJson(byte[] src, Class<T> clazz) {
        try {
            // 使用OBJECT_MAPPER的readValue方法将JSON数据字节数组转换为指定的Java对象
            return OBJECT_MAPPER.readValue(src, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 将JSON数据从字节数组转换成Java对象
     *
     * @param src 要转换的JSON数据字节数组
     * @param ref 用于指定目标类型的TypeReference对象，能够在运行时获取泛型类型的具体信息
     * @param <T> 转换后的Java对象
     * @return 目标Java对象的类型
     */
    public static <T> T readJson(byte[] src, TypeReference<T> ref) {
        try {
            // 使用OBJECT_MAPPER的readValue方法将JSON数据字节数组转换为指定的Java对象
            return OBJECT_MAPPER.readValue(src, ref);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 将JSON字符串转换成Map类型的对象
     *
     * @param str 要转换的JSON字符串
     * @return 转换后的Map对象
     */
    public static Map<String, Object> readJsonAsMap(String str) {
        try {
            // 使用OBJECT_MAPPER的readValue方法将JSON字符串转换为Map类型的对象
            // 这里使用了TypeReference来指定目标类型为HashMap<String, Object>
            return OBJECT_MAPPER.readValue(str, new TypeReference<HashMap<String, Object>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
