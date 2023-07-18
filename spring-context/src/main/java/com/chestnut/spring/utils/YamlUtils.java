package com.chestnut.spring.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Yaml文件工具类
 *
 * @author: Chestnut
 * @since: 2023-07-13
 **/
@SuppressWarnings("unused")
public class YamlUtils {
    /**
     * 从指定路径加载YAML文件，并将其解析为Map形式的对象
     *
     * @param path YAML文件的路径
     * @return 解析后的Map对象
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYaml(String path) {
        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions dumperOptions = new DumperOptions();
        Representer representer = new Representer(dumperOptions);
        NoImplicitResolver resolver = new NoImplicitResolver();
        Yaml yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);
        return ClassPathUtils.readInputStream(path, inputStream -> (Map<String, Object>) yaml.load(inputStream));
    }

    /**
     * 从指定路径加载YAML文件，并将其解析为扁平化的Map对象
     *
     * @param path YAML文件的路径
     * @return 解析后的扁平化Map对象
     */
    public static Map<String, Object> loadYamlAsPlainMap(String path) {
        Map<String, Object> data = loadYaml(path);
        Map<String, Object> plainData = new LinkedHashMap<>();
        convertTo(data, "", plainData);
        return plainData;
    }

    /**
     * 将源Map中的数据转换为扁平化的形式，存储到目标Map中
     *
     * @param source    源Map
     * @param prefix    键的前缀
     * @param plainData 目标Map，存储扁平化后的数据
     */
    private static void convertTo(Map<String, Object> source, String prefix, Map<String, Object> plainData) {
        for (String key : source.keySet()) {
            Object value = source.get(key);
            if (value instanceof Map) {
                // 递归调用convertTo方法处理嵌套的子Map
                @SuppressWarnings("unckecked")
                Map<String, Object> subMap = (Map<String, Object>) value;
                convertTo(subMap, prefix + key + '.', plainData);
            } else if (value instanceof List) {
                // 对于List类型的值，直接存储到目标Map中
                plainData.put(prefix + key, value);
            } else {
                // 将其他类型的值转换为字符串，并存储到目标Map中
                plainData.put(prefix + key, value.toString());
            }
        }
    }
}


/**
 * 自定义的YAML解析器，禁用所有的隐式类型转换，并将所有的值都视为字符串处理
 */
class NoImplicitResolver extends Resolver {
    public NoImplicitResolver() {
        super();
        super.yamlImplicitResolvers.clear();
    }
}
