package com.chestnut.spring.web;

import com.chestnut.spring.exception.ServerErrorException;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.*;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.Objects;

/**
 * FreeMarker视图解析器，用于将控制器层返回的视图逻辑名称解析为实际的视图对象，从而实现视图和模型数据的合并和渲染
 * 主要作用：
 * 1. 根据视图逻辑名称解析对应的FreeMarker模板文件。
 * 2. 提供模板的加载、渲染和生成响应内容的功能。
 * 3. 与Spring MVC框架无缝集成，将模型数据与视图模板进行合并和渲染。
 *
 * @author: Chestnut
 * @since: 2023-07-22
 **/
public class FreeMarkerViewResolver implements ViewResolver {
    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * Servlet上下文，用于获取资源信息
     */
    private final ServletContext servletContext;
    /**
     * FreeMarker模板文件路径
     */
    private final String templatePath;
    /**
     * FreeMarker模板文件编码方式
     */
    private final String templateEncoding;
    /**
     * FreeMarker配置对象
     */
    private Configuration config;

    /**
     * 创建一个FreeMarker视图解析器
     *
     * @param servletContext   Servlet上下文
     * @param templatePath     FreeMarker模板文件路径
     * @param templateEncoding FreeMarker模板文件编码方式
     */
    public FreeMarkerViewResolver(ServletContext servletContext, String templatePath, String templateEncoding) {
        this.servletContext = servletContext;
        this.templatePath = templatePath;
        this.templateEncoding = templateEncoding;
    }

    /**
     * 初始化FreeMarker视图解析器
     */
    @Override
    public void init() {
        logger.info("init {}, set template path: {}", getClass().getSimpleName(), this.templatePath);
        // 创建 FreeMarker 的配置对象，用于配置模板渲染的相关设置
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);

        // 设置输出格式为 HTML，以便输出内容符合 HTML 标准
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        // 设置模板文件的默认字符编码，用于读取模板文件时进行字符解码
        cfg.setDefaultEncoding(this.templateEncoding);
        // 设置模板加载器，用于加载视图模板文件
        cfg.setTemplateLoader(new ServletTemplateLoader(this.servletContext, this.templatePath));
        // 设置模板异常处理器，用于处理在模板渲染过程中可能出现的异常
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        // 设置自动转义策略，以防止在输出模板变量时出现 HTML 转义问题
        cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);
        // 禁用本地化查找，以确保模板在不同语言环境下都使用相同的查找规则
        cfg.setLocalizedLookup(false);

        // 创建 FreeMarker 默认对象包装器
        DefaultObjectWrapper ow = new DefaultObjectWrapper(Configuration.VERSION_2_3_32);
        // 配置是否暴露字段，以便模板可以访问对象的字段值
        ow.setExposeFields(true);
        // 设置 FreeMarker 默认对象包装器
        cfg.setObjectWrapper(ow);

        // 将配置对象保存在类成员变量中，以便后续模板渲染时使用
        this.config = cfg;
    }

    /**
     * 渲染视图的方法，将指定的视图名和模型数据渲染成实际的响应内容并发送给客户端
     *
     * @param viewName 视图名，用于表示要渲染的前端视图的逻辑名称
     * @param model    模型数据，一个包含键值对的Map对象，用于传递给视图进行渲染
     * @param request  客户端发送的HTTP请求对象
     * @param response 服务端发送的HTTP响应对象
     * @throws ServletException 如果在Servlet处理过程中发生异常
     * @throws IOException      如果在处理请求和响应时发生I/O异常
     */
    @Override
    public void render(String viewName, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Template templ = null;
        try {
            // 获取视图模板对象
            templ = this.config.getTemplate(viewName);
        } catch (Exception e) {
            // 如果视图模板不存在，抛出自定义的 ServerErrorException 异常
            throw new ServerErrorException("View not found: " + viewName);
        }

        // 获取 HTTP 响应对象的输出流，用于输出渲染后的内容
        PrintWriter pw = response.getWriter();
        try {
            // 渲染模板并将结果输出到响应的输出流中
            templ.process(model, pw);
        } catch (TemplateException e) {
            // 如果在渲染模板时出现异常，抛出自定义的 ServerErrorException 异常
            throw new ServerErrorException(e);
        }
        // 刷新输出流，确保渲染后的内容已经输出到响应中
        pw.flush();
    }
}


/**
 * 实现FreeMarker模板加载器（TemplateLoader）接口的Servlet模板加载器类
 * 主要作用：
 * 1.根据传入的模板名称，从Servlet上下文中查找对应的模板文件，并返回相应的模板读取器。
 * 2.然后，FreeMarker 引擎会使用该读取器加载模板内容，进而生成最终的响应内容。
 * copied from freemarker.cache.WebappTemplateLoader and modified to use
 * jakarta.servlet.ServletContext. Because it is used old javax.servlet.ServletContext.
 */
class ServletTemplateLoader implements TemplateLoader {
    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * Servlet上下文，用于获取资源信息
     */
    private final ServletContext servletContext;
    /**
     * FreeMarker模板文件路径，即模板文件所在的子目录路径
     */
    private final String subdirPath;

    /**
     * 创建一个Servlet模板加载器实例
     *
     * @param servletContext Servlet上下文，用于获取资源信息
     * @param subdirPath     模板文件所在的子目录路径
     */
    public ServletTemplateLoader(ServletContext servletContext, String subdirPath) {
        Objects.requireNonNull(servletContext);
        Objects.requireNonNull(subdirPath);
        // 规范化子目录路径的格式
        subdirPath = subdirPath.replace('\\', '/');
        if (!subdirPath.endsWith("/")) {
            subdirPath += "/";
        }
        if (!subdirPath.startsWith("/")) {
            subdirPath = "/" + subdirPath;
        }
        // 初始化类成员变量
        this.subdirPath = subdirPath;
        this.servletContext = servletContext;
    }

    /**
     * 在 Servlet上下文中查找指定模板名称的真实路径
     *
     * @param name 模板名称，用于查找对应的模板文件
     * @return 模板文件的File对象，如果找不到则返回null
     * @throws IOException 模板文件的File对象，如果找不到则返回null
     */
    @Override
    public Object findTemplateSource(String name) throws IOException {
        // 构建模板文件的完整路径
        String fullPath = subdirPath + name;
        try {
            // 获取模板文件的真实路径
            String realPath = servletContext.getRealPath(fullPath);
            logger.atDebug().log("load template {}: real path: {}", name, realPath);
            // 检查模板文件是否存在并可读
            if (realPath != null) {
                File file = new File(realPath);
                if (file.canRead() && file.isFile()) {
                    return file;
                }
            }
        } catch (SecurityException e) {
            ;// ignore
        }
        return null;
    }

    /**
     * 获取模板文件的最后修改时间
     *
     * @param templateSource 模板文件的File对象
     * @return 模板文件的最后修改时间，如果无法获取则返回0
     */
    @Override
    public long getLastModified(Object templateSource) {
        if (templateSource instanceof File) {
            return ((File) templateSource).lastModified();
        }
        return 0;
    }

    /**
     * 获取模板文件的读取器（Reader）
     *
     * @param templateSource 获取模板文件的读取器（Reader）
     * @param encoding       模板文件的字符编码
     * @return 模板文件的读取器（Reader）对象
     * @throws IOException 如果无法获取读取器或模板文件不存在
     */
    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        if (templateSource instanceof File) {
            return new InputStreamReader(new FileInputStream((File) templateSource), encoding);
        }
        throw new IOException("File not found.");
    }

    /**
     * 关闭模板文件资源
     *
     * @param templateSource 模板文件的File对象，将在此方法中关闭该资源（此处留空，无需实际关闭）
     * @throws IOException 如果在关闭模板文件资源时发生I/O异常，此处不会抛出异常
     */
    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        // 此处留空，因为模板文件资源的关闭不需要实际的操作。
        // 如果有需要在关闭模板文件资源时进行额外的操作，可以在此方法中实现。
    }

    /**
     * 获取是否使用URL连接的缓存
     *
     * @return 如果使用URL连接的缓存，则返回true，否则返回false
     */
    public Boolean getURLConnectionUsesCaches() {
        // 表示不使用缓存
        return Boolean.FALSE;
    }
}
