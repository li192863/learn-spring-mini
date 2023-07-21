package com.chestnut.spring.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 资源解析器，用于简单的类路径扫描工作，可以在目录和JAR文件中工作。
 * 只负责扫描并列出所有文件，由客户端决定是找出.class文件，还是找出.properties文件
 *
 * @author: Chestnut
 * @since: 2023-07-11
 **/
public class ResourceResolver {
    /**
     * 日志
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 基础包路径
     */
    private String basePackage;

    /**
     * 创建一个ResourceResolver实例
     * @param basePackage 基础包路径
     */
    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * 扫描指定包路径下的资源，并将其映射为指定类型的对象列表
     *
     * @param mapper 资源对象映射函数
     * @param <R>    结果对象的类型
     * @return 结果对象列表
     */
    public <R> List<R> scan(Function<Resource, R> mapper) {
        String basePackagePath = this.basePackage.replace(".", "/");
        String path = basePackagePath;
        try {
            List<R> collector = new ArrayList<>();
            scan0(basePackagePath, path, collector, mapper);
            return collector;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 实际执行扫描的内部方法，扫描指定路径下的资源文件，并将其收集到列表中
     *
     * @param basePackagePath 基础包路径
     * @param path            目标路径
     * @param collector       结果对象收集器
     * @param mapper          资源对象映射函数
     * @param <R>             结果对象的类型
     * @throws URISyntaxException
     * @throws IOException
     */
    private <R> void scan0(String basePackagePath, String path, List<R> collector, Function<Resource, R> mapper) throws URISyntaxException, IOException {
        logger.atDebug().log("scan path: {}", path);
        // 获取当前线程的上下文类加载器，并获取指定路径下的所有资源URL
        Enumeration<URL> en = getContextClassLoader().getResources(path);
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            URI uri = url.toURI();
            String uriStr = removeTrailingSlash(uriToString(uri));
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if (uriBaseStr.startsWith("file:")) {
                uriBaseStr = uriBaseStr.substring(5);
            }
            if (uriStr.startsWith("jar:")) {
                // 如果资源在JAR文件中，则进行JAR文件扫描
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
            } else {
                // 如果资源在文件系统中，则进行文件扫描
                scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
            }
        }
    }

    /**
     * 获取上下文类加载器
     *
     * @return 当前线程的上下文类加载器
     */
    private ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        // 获取当前线程的上下文类加载器
        cl = Thread.currentThread().getContextClassLoader();
        // 若未指定特定的上下文类加载器
        if (cl == null) {
            // 获取当前类（即包含该方法的类）的类加载器
            cl = getClass().getClassLoader();
        }
        return cl;
    }


    /**
     * 将JAR URI转换为路径
     *
     * @param basePackagePath 基础包路径
     * @param jarUri          JAR文件的URI
     * @return
     * @throws IOException
     */
    private Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        return FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
    }

    /**
     * 将File URI转换为字符串
     *
     * @param uri
     * @return
     */
    private String uriToString(URI uri) {
        return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8);
    }

    /**
     * 扫描文件并将其转换为资源对象，然后将资源对象应用于映射函数，并将结果收集到列表中。
     *
     * @param isJar
     * @param base
     * @param root
     * @param collector
     * @param mapper
     * @param <R>
     * @throws IOException
     */
    private <R> void scanFile(boolean isJar, String base, Path root, List<R> collector, Function<Resource, R> mapper) throws IOException {
        String baseDir = removeTrailingSlash(base);
        // 遍历指定根路径下的所有文件并进行处理
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if (isJar) {
                // 如果在JAR文件中，则使用文件相对路径创建资源对象
                res = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                // 如果在文件系统中，则使用文件绝对路径和相对路径创建资源对象
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                res = new Resource("file:" + path, name);
            }
            logger.atDebug().log("found resource: {}", res);
            // 将资源对象应用于映射函数，并将结果添加到收集器中
            R r = mapper.apply(res);
            if (r != null) {
                collector.add(r);
            }
        });
    }

    /**
     * 去除字符串开头的斜杠或反斜杠。
     *
     * @param s 要处理的字符串
     * @return 去除开头斜杠或反斜杠后的字符串
     */
    private String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * 去除字符串末尾的斜杠或反斜杠
     *
     * @param s 要处理的字符串
     * @return 去除末尾斜杠或反斜杠后的字符串
     */
    private String removeTrailingSlash(String s) {
        if (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
