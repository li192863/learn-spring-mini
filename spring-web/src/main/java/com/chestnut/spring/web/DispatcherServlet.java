package com.chestnut.spring.web;

import com.chestnut.spring.annotation.*;
import com.chestnut.spring.context.ApplicationContext;
import com.chestnut.spring.context.BeanDefinition;
import com.chestnut.spring.context.ConfigurableApplicationContext;
import com.chestnut.spring.exception.ErrorResponseException;
import com.chestnut.spring.exception.NestedRuntimeException;
import com.chestnut.spring.io.PropertyResolver;
import com.chestnut.spring.web.utils.JsonUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 调度器
 *
 * @author: Chestnut
 * @since: 2023-07-21
 **/
public class DispatcherServlet extends HttpServlet {
    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 应用程序上下文
     */
    private final ApplicationContext applicationContext;
    /**
     * 视图解析器
     */
    private final ViewResolver viewResolver;
    /**
     * 图标路径
     */
    private final String faviconPath;
    /**
     * 资源路径
     */
    private final String resourcePath;
    /**
     * Get请求处理器
     */
    private List<Dispatcher> getDispatchers = new ArrayList<>();
    /**
     * Post请求处理器
     */
    private List<Dispatcher> postDispatchers = new ArrayList<>();

    /**
     * 创建一个调度器实例
     *
     * @param applicationContext 应用程序上下文
     * @param propertyResolver   属性解析器
     */
    public DispatcherServlet(ApplicationContext applicationContext, PropertyResolver propertyResolver) {
        this.applicationContext = applicationContext;
        this.viewResolver = applicationContext.getBean(ViewResolver.class);
        this.faviconPath = propertyResolver.getProperty("${spring.web.favicon-path:/favicon.ico}");
        String resourcePath = propertyResolver.getProperty("${spring.web.static-path:/static/}");
        if (!resourcePath.endsWith("/")) {
            resourcePath = resourcePath + "/";
        }
        this.resourcePath = resourcePath;
    }

    /**
     * 初始调度器实例的方法
     *
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    @Override
    public void init() throws ServletException {
        logger.info("init {}.", getClass().getName());
        // 扫描@Controller与@RestController
        List<BeanDefinition> defs = ((ConfigurableApplicationContext) this.applicationContext).findBeanDefinitions(Object.class);
        for (BeanDefinition def : defs) {
            // 获取定义基本信息
            Class<?> beanClass = def.getBeanClass();
            String name = def.getName();
            Object controllerInstance = def.getRequiredInstance();
            Class<?> instanceClass = controllerInstance.getClass();

            // 添加MVC Dispatchers
            Controller controller = beanClass.getAnnotation(Controller.class);
            if (controller != null) {
                logger.info("add MVC controller '{}': {}", name, instanceClass.getName());
                addDispatchers(false, instanceClass, controllerInstance);
            }
            // 添加REST Dispatchers
            RestController restController = beanClass.getAnnotation(RestController.class);
            if (restController != null) {
                logger.info("add REST controller '{}': {}", name, instanceClass.getName());
                addDispatchers(true, instanceClass, controllerInstance);
            }
        }
    }

    /**
     * 销毁调度器实例的方法
     */
    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    /**
     * 添加方法
     *
     * @param isRest     是否为RestController
     * @param type       类型
     * @param controller 实例
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    private void addDispatchers(boolean isRest, Class<?> type, Object controller) throws ServletException {
        // 在当前类查找 Method 并添加调度器
        for (Method method : type.getDeclaredMethods()) {
            // 获取方法基本信息
            int mod = method.getModifiers();

            // 添加Get Dispatchers
            GetMapping get = method.getAnnotation(GetMapping.class);
            if (get != null) {
                if (Modifier.isStatic(mod)) {
                    throw new ServletException("Cannot do URL mapping to static method: " + method);
                }
                method.setAccessible(true);
                this.getDispatchers.add(new Dispatcher(isRest, controller, method, get.value()));
            }
            // 添加Post Dispatchers
            PostMapping post = method.getAnnotation(PostMapping.class);
            if (post != null) {
                if (Modifier.isStatic(mod)) {
                    throw new ServletException("Cannot do URL mapping to static method: " + method);
                }
                method.setAccessible(true);
                this.postDispatchers.add(new Dispatcher(isRest, controller, method, post.value()));
            }
        }
        // 在父类查找 Method 并添加调度器
        // If this Class object represents either the Object class, an interface, a primitive type, or void, then null is returned
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            addDispatchers(isRest, superClass, controller);
        }
    }

    /**
     * 处理HTTP GET请求的方法
     *
     * @param request  客户端发送的HTTP请求对象
     * @param response 服务端发送的HTTP响应对象
     * @throws ServletException 如果在Servlet处理过程中发生异常
     * @throws IOException      如果在处理请求和响应时发生I/O异常
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = request.getRequestURI();
        if (url.startsWith(this.resourcePath) || url.equals(this.faviconPath)) {
            // 处理资源
            doResource(url, request, response);
        } else {
            // 处理服务
            doService(this.getDispatchers, request, response);
        }
    }

    /**
     * 处理HTTP POST请求的方法
     *
     * @param request  客户端发送的HTTP请求对象
     * @param response 服务端发送的HTTP响应对象
     * @throws ServletException 如果在Servlet处理过程中发生异常
     * @throws IOException      如果在处理请求和响应时发生I/O异常
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 处理服务
        doService(this.postDispatchers, request, response);
    }

    /**
     * 处理资源，从ServletContext中获取指定资源并输出到HttpServletResponse中
     *
     * @param url      资源的URL路径
     * @param request  客户端发送的HTTP请求对象
     * @param response 服务端发送的HTTP响应对象
     * @throws IOException 如果在处理请求和响应时发生I/O异常
     */
    private void doResource(String url, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 从HttpServletRequest中获取ServletContext对象
        ServletContext ctx = request.getServletContext();
        try (InputStream input = ctx.getResourceAsStream(url)) {
            if (input == null) {
                response.sendError(404, "Not Found");
                return;
            }
            // 从URL中获取文件名
            String file = url;
            int n = url.lastIndexOf('/');
            if (n >= 0) {
                file = url.substring(n + 1);
            }
            // 获取文件的MIME类型
            String mime = ctx.getMimeType(file);
            if (mime == null) {
                // 如果未找到MIME类型，则使用默认的二进制流类型
                mime = "application/octet-stream";
            }
            // 设置HTTP响应的Content-Type头部
            response.setContentType(mime);
            // 获取HttpServletResponse的输出流
            ServletOutputStream output = response.getOutputStream();
            // 将资源内容从输入流拷贝到输出流
            input.transferTo(output);
            // 刷新输出流
            output.flush();
        }
    }

    /**
     * 处理HTTP请求并进行请求分发处理
     *
     * @param dispatchers 处理器列表
     * @param request     客户端发送的HTTP请求对象
     * @param response    服务端发送的HTTP响应对象
     * @throws IOException      如果在处理请求和响应时发生I/O异常
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    private void doService(List<Dispatcher> dispatchers, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String uri = request.getRequestURI();
        try {
            // 调用方法进行请求分发处理
            dispatch(dispatchers, request, response);
        } catch (ErrorResponseException e) {
            // 捕获 ClientErrorException 与 ServerErrorException 并处理，其余异常直接抛出
            logger.warn("process request failed with status " + e.statusCode + " : " + uri, e);
            // 如果响应尚未提交，则重置缓冲区并发送错误状态码到客户端
            if (!response.isCommitted()) {
                response.resetBuffer();
                response.sendError(e.statusCode);
            }
        } catch (RuntimeException | ServletException | IOException e) {
            logger.warn("process request failed: " + uri, e);
            throw e;
        } catch (Exception e) {
            logger.warn("process request failed: " + uri, e);
            throw new NestedRuntimeException(e);
        }
    }

    /**
     * 处理服务，从Dispatchers中获取指定处理器处理
     *
     * @param dispatchers 处理器列表
     * @param request     客户端发送的HTTP请求对象
     * @param response    服务端发送的HTTP响应对象
     * @throws Exception 处理过程中出现异常
     */
    private void dispatch(List<Dispatcher> dispatchers, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getRequestURI();
        // 遍历所有的调度器进行处理
        for (Dispatcher dispatcher : dispatchers) {
            Result result = dispatcher.process(uri, request, response);
            if (result.processed()) {
                if (dispatcher.isRest) {
                    handleRestResult(dispatcher, uri, result.returnObject(), request, response);
                } else {
                    handleNonRestResult(dispatcher, uri, result.returnObject(), request, response);
                }
                return;
            }
        }
        // not found:
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
    }

    /**
     * 处理REST结果的方法
     *
     * @param dispatcher 处理器
     * @param uri        当前处理的请求URI
     * @param returnObj  处理结果对象
     * @param request    客户端发送的HTTP请求对象
     * @param response   服务端发送的HTTP响应对象
     * @throws IOException      如果在处理请求和响应时发生I/O异常
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    private void handleRestResult(Dispatcher dispatcher, String uri, Object returnObj, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // 设置内容格式为"application/json"
        if (!response.isCommitted()) {
            response.setContentType("application/json");
        }
        if (dispatcher.isResponseBody) {
            // 方法上有@ResponseBody注解
            if (returnObj instanceof String s) {
                sendResponse(response, s);
            } else if (returnObj instanceof byte[] data) {
                sendResponse(response, data);
            } else {
                throw new ServletException("Unable to process REST result when handle url: " + uri);
            }
        } else if (!dispatcher.isVoid) {
            // 方法返回对象，将其转为JSON后发送
            PrintWriter pw = response.getWriter();
            JsonUtils.writeJson(pw, returnObj);
            pw.flush();
        }
    }

    /**
     * 处理非REST结果的方法
     *
     * @param dispatcher 处理器
     * @param uri        当前处理的请求URI
     * @param returnObj  处理结果对象
     * @param request    客户端发送的HTTP请求对象
     * @param response   服务端发送的HTTP响应对象
     * @throws IOException      如果在处理请求和响应时发生I/O异常
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    private void handleNonRestResult(Dispatcher dispatcher, String uri, Object returnObj, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // 设置内容格式为"text/html"
        if (!response.isCommitted()) {
            response.setContentType("text/html");
        }
        if (returnObj instanceof String s) {
            // 返回对象为字符串
            if (dispatcher.isResponseBody) {
                sendResponse(response, s);
            } else if (s.startsWith("redirect:")) {
                response.sendRedirect(s.substring(9));
            } else {
                throw new ServletException("Unable to process String result when handle url: " + uri);
            }
        } else if (returnObj instanceof byte[] data) {
            // 返回对象为二进制字节
            if (dispatcher.isResponseBody) {
                sendResponse(response, data);
            } else {
                throw new ServletException("Unable to process byte[] result when handle url: " + uri);
            }
        } else if (returnObj instanceof ModelAndView mv) {
            // 返回对象为模型视图
            String view = mv.getViewName();
            if (view.startsWith("redirect:")) {
                response.sendRedirect(view.substring(9));
            } else {
                this.viewResolver.render(view, mv.getModel(), request, response);
            }
        } else if (!dispatcher.isVoid && returnObj != null) {
            // 返回对象且不为空
            throw new ServletException("Unable to process " + returnObj.getClass().getName() + " result when handle url: " + uri);
        }
    }

    /**
     * 使用PrintWriter向客户端发送数据
     *
     * @param response 服务端发送的HTTP响应对象
     * @param data     要发送的数据
     * @throws IOException 如果在处理请求和响应时发生I/O异常
     */
    private void sendResponse(HttpServletResponse response, String data) throws IOException {
        PrintWriter pw = response.getWriter();
        pw.write(data);
        pw.flush();
    }

    /**
     * 使用ServletOutputStream向客户端发送数据
     *
     * @param response 服务端发送的HTTP响应对象
     * @param data     要发送的数据
     * @throws IOException 如果在处理请求和响应时发生I/O异常
     */
    private void sendResponse(HttpServletResponse response, byte[] data) throws IOException {
        ServletOutputStream output = response.getOutputStream();
        output.write(data);
        output.flush();
    }
}
