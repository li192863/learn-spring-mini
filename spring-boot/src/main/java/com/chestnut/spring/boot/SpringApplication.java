package com.chestnut.spring.boot;

import com.chestnut.spring.io.PropertyResolver;
import com.chestnut.spring.utils.ClassPathUtils;
import com.chestnut.spring.utils.YamlUtils;
import com.chestnut.spring.web.ContextLoaderInitializer;
import org.apache.catalina.Context;
import org.apache.catalina.Server;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Spring应用程序启动类
 *
 * @author: Chestnut
 * @since: 2023-07-24
 **/
public class SpringApplication {
    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 应用配置文件
     */
    private static final String CONFIG_APP_YAML = "/application.yml";
    /**
     * 应用配置属性
     */
    private static final String CONFIG_APP_PROP = "/application.properties";

    /**
     * 启动应用
     *
     * @param webDir      Web目录
     * @param baseDir     应用基目录
     * @param configClass 应用配置类
     * @param args        启动参数
     * @throws Exception 可能抛出的异常
     */
    public static void run(String webDir, String baseDir, Class<?> configClass, String... args) throws Exception {
        new SpringApplication().start(webDir, baseDir, configClass, args);
    }

    /**
     * 启动应用
     *
     * @param webDir      Web目录
     * @param baseDir     应用基目录
     * @param configClass 应用配置类
     * @param args        启动参数
     * @throws Exception 可能抛出的异常
     */
    public void start(String webDir, String baseDir, Class<?> configClass, String... args) throws Exception {
        printBanner();

        // 记录启动信息
        final long startTime = System.currentTimeMillis();
        final int javaVersion = Runtime.version().feature();
        final long pid = ManagementFactory.getRuntimeMXBean().getPid();
        final String user = System.getProperty("user.name");
        final String pwd = Paths.get("").toAbsolutePath().toString();
        logger.info("Starting {} using Java {} with PID {} (started by {} in {})", configClass.getSimpleName(), javaVersion, pid, user, pwd);
        // 创建属性解析器
        PropertyResolver propertyResolver = createPropertyResolver();
        // 启动Tomcat服务器
        Server server = startTomcat(webDir, baseDir, configClass, propertyResolver);
        // 记录启动信息
        final long endTime = System.currentTimeMillis();
        final String appTime = String.format("%.3f", (endTime - startTime) / 1000.0);
        final String jvmTime = String.format("%.3f", ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0);
        logger.info("Started {} in {} seconds (process running for {})", configClass.getSimpleName(), appTime, jvmTime);

        server.await();
    }

    /**
     * 启动Tomcat服务器
     *
     * @param webDir            Web目录
     * @param baseDir           应用基目录
     * @param configClass       应用配置类
     * @param propertyResolver  属性解析器
     * @return Tomcat服务器实例
     * @throws Exception 可能抛出的异常
     */
    protected Server startTomcat(String webDir, String baseDir, Class<?> configClass, PropertyResolver propertyResolver) throws Exception {
        int port = propertyResolver.getProperty("${server.port:8080}", int.class);
        logger.info("starting Tomcat at port {}...", port);
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector().setThrowOnFailure(true);
        Context ctx = tomcat.addWebapp("", new File(webDir).getAbsolutePath());
        WebResourceRoot resources = new StandardRoot(ctx);
        // 添加WEB-INF/classes目录到资源路径，以便加载应用的类文件
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", new File(baseDir).getAbsolutePath(), "/"));
        ctx.setResources(resources);
        ctx.addServletContainerInitializer(new ContextLoaderInitializer(configClass, propertyResolver), Set.of());
        tomcat.start();
        logger.info("Tomcat started at port {}...", port);
        return tomcat.getServer();
    }

    /**
     * 打印应用的Banner
     */
    protected void printBanner() {
        String banner = ClassPathUtils.readString("/banner.txt");
        banner.lines().forEach(System.out::println);
    }

    /**
     * 创建一个属性解析器
     * 将从/application.yml或/application.properties中读取
     *
     * @return 属性解析器
     */
    private PropertyResolver createPropertyResolver() {
        final Properties props = new Properties();
        try {
            // 尝试加载application.yml
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            logger.info("load config: {}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if (value instanceof String strValue) {
                    props.put(key, strValue);
                }
            }
        } catch (UncheckedIOException e) {
            // 尝试加载application.properties
            if (e.getCause() instanceof FileNotFoundException) {
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, inputStream -> {
                    logger.info("load config: {}", CONFIG_APP_PROP);
                    props.load(inputStream);
                    return true;
                });
            }
        }
        return new PropertyResolver(props);
    }
}
