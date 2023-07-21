package com.chestnut.spring.jdbc;

import com.chestnut.spring.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean的行映射类，用于将数据库查询结果映射为JavaBean对象
 *
 * @author: Chestnut
 * @since: 2023-07-21
 **/
public class BeanRowMapper<T> implements RowMapper<T> {
    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 类
     */
    private Class<T> clazz;
    /**
     * 构造方法
     */
    private Constructor<T> constructor;
    /**
     * 属性
     */
    private Map<String, Field> fields = new HashMap<>();
    /**
     * 方法
     */
    private Map<String, Method> methods = new HashMap<>();

    /**
     * 创建一个BeanRowMapper对象
     *
     * @param clazz 映射的目标JavaBean对象的类型的泛型参数
     */
    public BeanRowMapper(Class<T> clazz) {
        // 获取目标JavaBean对象的类
        this.clazz = clazz;
        try {
            // 获取目标JavaBean对象的公共默认（无参）构造函数
            this.constructor = clazz.getConstructor();
        } catch (ReflectiveOperationException e) {
            throw new DataAccessException(String.format("No public default constructor found for class %s when build BeanRowMapper.", clazz.getName()), e);
        }
        // 遍历目标JavaBean对象的字段，并将字段名与字段对象放入fields映射中
        for (Field f : clazz.getFields()) {
            String name = f.getName();
            this.fields.put(name, f);
            logger.atDebug().log("Add row mapping: {} to field {}", name, name);
        }
        // 遍历目标JavaBean对象的方法，并将setter方法与数据库列名映射放入methods映射中
        for (Method m : clazz.getMethods()) {
            Parameter[] ps = m.getParameters();
            if (ps.length == 1) {
                String name = m.getName();
                if (name.length() >= 4 && name.startsWith("set")) {
                    String prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    this.methods.put(prop, m);
                    logger.atDebug().log("Add row mapping: {} to {}({})", prop, name, ps[0].getType().getSimpleName());
                }
            }
        }
    }

    /**
     * 将数据库查询结果集中的一行数据映射为对象
     *
     * @param rs     数据库查询结果集对象，包含查询的结果数据
     * @param rowNum 行号，表示当前处理的结果集行数（从1开始）
     * @return 映射后的目标对象
     * @throws SQLException 如果在映射的过程中出现SQL异常
     */
    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T bean = null;
        try {
            // 使用构造函数创建目标 JavaBean 对象
            bean = this.constructor.newInstance();
            // 获取结果集的元数据，包含查询的列信息
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            // 遍历结果集的每一列
            for (int i = 1; i <= columns; i++) {
                // 获取列名
                String label = underscoreToCamel(meta.getColumnLabel(i));
                // 检查是否存在该列名对应的 setter 方法
                Method method = this.methods.get(label);
                if (method != null) {
                    // 通过 setter 方法设置目标 JavaBean 对象的属性值
                    method.invoke(bean, rs.getObject(label));
                } else {
                    // 如果没有找到对应的 setter 方法，则尝试查找是否存在对应的字段
                    Field field = this.fields.get(label);
                    if (field != null) {
                        // 通过字段设置目标 JavaBean 对象的属性值
                        field.set(bean, rs.getObject(label));
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new DataAccessException(String.format("Could not map result set to class %s", this.clazz.getName()), e);
        }
        return bean;
    }

    /**
     * 将下划线命名转化为驼峰命名
     *
     * @param underscoreName 下划线命名
     * @return 转化后的驼峰命名
     */
    private String underscoreToCamel(String underscoreName) {
        StringBuilder result = new StringBuilder();
        String[] parts = underscoreName.split("_");
        for (String part : parts) {
            if (result.length() == 0) {
                result.append(part);
            } else {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return result.toString();
    }
}
