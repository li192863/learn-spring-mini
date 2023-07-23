package com.chestnut.spring.web;

import com.chestnut.spring.annotation.*;
import com.chestnut.spring.context.ApplicationContext;
import com.chestnut.spring.context.BeanDefinition;
import com.chestnut.spring.context.ConfigurableApplicationContext;
import com.chestnut.spring.exception.ErrorResponseException;
import com.chestnut.spring.exception.NestedRuntimeException;
import com.chestnut.spring.exception.ServerErrorException;
import com.chestnut.spring.exception.ServerWebInputException;
import com.chestnut.spring.io.PropertyResolver;
import com.chestnut.spring.utils.ClassUtils;
import com.chestnut.spring.web.utils.JsonUtils;
import com.chestnut.spring.web.utils.PathUtils;
import com.chestnut.spring.web.utils.WebUtils;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private ApplicationContext applicationContext;
    /**
     * 视图解析器
     */
    private ViewResolver viewResolver;
    /**
     * 资源路径
     */
    private String resourcePath;
    /**
     * 图标路径
     */
    private String faviconPath;
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
        this.resourcePath = propertyResolver.getProperty("${spring.web.static-path:/static/}");
        this.faviconPath = propertyResolver.getProperty("${spring.web.favicon-path:/favicon.ico}");
        if (!this.resourcePath.endsWith("/")) {
            this.resourcePath = this.resourcePath + "/";
        }
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
            Class<?> beanClass = def.getBeanClass();
            Controller controller = beanClass.getAnnotation(Controller.class);
            RestController restController = beanClass.getAnnotation(RestController.class);
            if (controller != null && restController != null) {
                throw new ServletException("Found @Controller and @RestController on class: " + beanClass.getName());
            }

            String name = def.getName();
            Object bean = def.getRequiredInstance();
            if (controller != null) {
                addController(false, name, bean);
            }
            if (restController != null) {
                addController(true, name, bean);
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
     * 添加控制器
     *
     * @param isRest   是否为RestController
     * @param name     名称
     * @param instance 实例
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    private void addController(boolean isRest, String name, Object instance) throws ServletException {
        logger.info("add {} controller '{}': {}", isRest ? "REST" : "MVC", name, instance.getClass().getName());
        addMethods(isRest, name, instance, instance.getClass());
    }

    /**
     * 添加方法
     *
     * @param isRest   是否为RestController
     * @param name     名称
     * @param instance 实例
     * @param type     类型
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    private void addMethods(boolean isRest, String name, Object instance, Class<?> type) throws ServletException {
        for (Method m : type.getDeclaredMethods()) {
            GetMapping get = m.getAnnotation(GetMapping.class);
            if (get != null) {
                checkMethod(m);
                m.setAccessible(true);
                this.getDispatchers.add(new Dispatcher(isRest, instance, m, get.value()));
            }
            PostMapping post = m.getAnnotation(PostMapping.class);
            if (post != null) {
                checkMethod(m);
                m.setAccessible(true);
                this.postDispatchers.add(new Dispatcher(isRest, instance, m, post.value()));
            }
        }
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            addMethods(isRest, name, instance, superClass);
        }
    }

    /**
     * 检查方法
     *
     * @param m 方法
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    private void checkMethod(Method m) throws ServletException {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new ServletException("Cannot do URL mapping to static method: " + m);
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
        if (url.equals(this.faviconPath) || url.startsWith(this.resourcePath)) {
            // 处理资源
            doResource(url, request, response);
        } else {
            // 处理服务
            doService(request, response, this.getDispatchers);
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
        doService(request, response, this.postDispatchers);
    }

    /**
     * 处理HTTP请求并进行请求分发处理
     *
     * @param req         客户端发送的HTTP请求对象
     * @param resp        服务端发送的HTTP响应对象
     * @param dispatchers 处理器列表
     * @throws IOException      如果在处理请求和响应时发生I/O异常
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    private void doService(HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws IOException, ServletException {
        String uri = req.getRequestURI();
        try {
            // 调用重载的doService方法，进行请求分发处理
            doService(uri, req, resp, dispatchers);
        } catch (ErrorResponseException e) {
            logger.warn("process request failed with status " + e.statusCode + " : " + uri, e);
            // 如果响应尚未提交，则重置缓冲区并发送错误状态码到客户端
            if (!resp.isCommitted()) {
                resp.resetBuffer();
                resp.sendError(e.statusCode);
            }
        } catch (RuntimeException | ServletException | IOException e) {
            logger.warn("process request failed: " + uri, e);
            throw e;
        } catch (Exception e) {
            logger.warn("process request failed: " + uri, e);
            throw new NestedRuntimeException(e);
        }
    }

    private void doService(String url, HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws Exception {
        for (Dispatcher dispatcher: dispatchers) {
            Result result = dispatcher.process(url, req, resp);
            if (!result.processed()) {
                continue;
            }
            Object r = result.returnObject();
            if (dispatcher.isRest) {
                // REST风格请求
                if (!resp.isCommitted()) {
                    resp.setContentType("application/json");
                }
                if (dispatcher.isResponseBody) {
                    if (r instanceof String s) {
                        // send as response body:
                        PrintWriter pw = resp.getWriter();
                        pw.write(s);
                        pw.flush();
                    } else if (r instanceof byte[] data) {
                        // send as response body:
                        ServletOutputStream output = resp.getOutputStream();
                        output.write(data);
                        output.flush();
                    } else {
                        // error:
                        throw new ServletException("Unable to process REST result when handle url: " + url);
                    }
                } else if (!dispatcher.isVoid) {
                    PrintWriter pw = resp.getWriter();
                    JsonUtils.writeJson(pw, r);
                    pw.flush();
                }
            } else {
                // 非REST风格请求
                if (!resp.isCommitted()) {
                    resp.setContentType("text/html");
                }
                if (r instanceof String s) {
                    if (dispatcher.isResponseBody) {
                        // send as response body:
                        PrintWriter pw = resp.getWriter();
                        pw.write(s);
                        pw.flush();
                    } else if (s.startsWith("redirect:")) {
                        // send redirect:
                        resp.sendRedirect(s.substring(9));
                    } else {
                        // error:
                        throw new ServletException("Unable to process String result when handle url: " + url);
                    }
                } else if (r instanceof byte[] data) {
                    if (dispatcher.isResponseBody) {
                        // send as response body:
                        ServletOutputStream output = resp.getOutputStream();
                        output.write(data);
                        output.flush();
                    } else {
                        // error:
                        throw new ServletException("Unable to process byte[] result when handle url: " + url);
                    }
                } else if (r instanceof ModelAndView mv) {
                    String view = mv.getViewName();
                    if (view.startsWith("redirect:")) {
                        // send redirect:
                        resp.sendRedirect(view.substring(9));
                    } else {
                        this.viewResolver.render(view, mv.getModel(), req, resp);
                    }
                } else if (!dispatcher.isVoid && r != null) {
                    // error:
                    throw new ServletException("Unable to process " + r.getClass().getName() + " result when handle url: " + url);
                }
            }
            return;
        }
        // not found:
        resp.sendError(404, "Not Found");
    }

    /**
     * 从ServletContext中获取指定资源并输出到HttpServletResponse中
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
            } else {
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
    }

    /**
     * 处理特定URL的处理器
     */
    static class Dispatcher {
        /**
         * 日志记录器
         */
        final Logger logger = LoggerFactory.getLogger(getClass());
        /**
         * 结果
         */
        final static Result NOT_PROCESSED = new Result(false, null);

        /**
         * 是否为REST风格的请求
         */
        boolean isRest;
        /**
         * 是否有@ResponseBody
         */
        boolean isResponseBody;
        /**
         * 是否返回void
         */
        boolean isVoid;
        /**
         * URL正则匹配
         */
        Pattern urlPattern;
        /**
         * 控制器
         */
        Object controller;
        /**
         * 控制器方法
         */
        Method handlerMethod;
        /**
         * 控制器方法参数
         */
        Param[] methodParameters;

        /**
         * 创建一个Dispatcher对象
         *
         * @param isRest        是否为REST风格的请求
         * @param controller    控制器
         * @param handlerMethod 控制器方法
         * @param urlPath       URL路径
         * @throws ServletException 如果在Servlet处理过程中发生异常
         */
        public Dispatcher(boolean isRest, Object controller, Method handlerMethod, String urlPath) throws ServletException {
            this.isRest = isRest;
            this.isResponseBody = handlerMethod.getAnnotation(ResponseBody.class) != null;
            this.isVoid = handlerMethod.getReturnType() == void.class;
            this.urlPattern = PathUtils.compile(urlPath);
            this.controller = controller;
            this.handlerMethod = handlerMethod;
            // 控制器方法参数
            Parameter[] params = handlerMethod.getParameters();
            Annotation[][] paramsAnnos = handlerMethod.getParameterAnnotations();
            this.methodParameters = new Param[params.length];
            for (int i = 0; i < params.length; i++) {
                this.methodParameters[i] = new Param(handlerMethod, params[i], paramsAnnos[i]);
            }

            // 使用日志记录处理请求方法的映射信息
            logger.atDebug().log("mapping {} to handler {}.{}", urlPath, controller.getClass().getSimpleName(), handlerMethod.getName());
            // 如果日志级别为DEBUG，则记录处理请求方法的参数信息
            if (logger.isDebugEnabled()) {
                for (Param p : this.methodParameters) {
                    logger.debug("> parameter: {}", p);
                }
            }
        }

        /**
         * 处理传入的URL，执行对应的方法，并返回处理结果
         *
         * @param url      URL路径
         * @param request  客户端发送的HTTP请求对象
         * @param response 服务端发送的HTTP响应对象
         * @return 处理结果对象
         * @throws Exception 如果处理过程中发生异常
         */
        public Result process(String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
            // 使用urlPattern正则表达式匹配传入的URL
            Matcher matcher = this.urlPattern.matcher(url);
            boolean isMatch = matcher.matches();
            // 如果URL未匹配成功，则返回默认未处理结果
            if (!isMatch) {
                return NOT_PROCESSED;
            }

            // 创建方法参数
            Object[] arguments = new Object[this.methodParameters.length];
            for (int i = 0; i < arguments.length; i++) {
                Param param = methodParameters[i];
                // 根据参数的类型进行不同的处理
                arguments[i] = switch (param.paramType) {
                    case PATH_VARIABLE -> getParamFromPathVariable(matcher, param);
                    case REQUEST_BODY -> getParamFromRequestBody(request, param);
                    case REQUEST_PARAM -> getParamFromRequestParam(request, param);
                    case SERVLET_VARIABLE -> getParamFromServletVariable(request, response, param);
                };
            }

            // 获取处理结果
            Object result = null;
            try {
                result = this.handlerMethod.invoke(this.controller, arguments);
            } catch (InvocationTargetException e) {
                Throwable t = e.getCause();
                // 普通的异常
                if (t instanceof Exception ex) {
                    throw ex;
                }
                throw e;
            } catch (ReflectiveOperationException e) {
                throw new ServerErrorException(e);
            }
            // 返回处理结果对象
            return new Result(true, result);
        }

        /**
         * 处理@PathVariable
         *
         * @param matcher 匹配器
         * @param param   参数
         * @return 处理后的对象
         */
        private Object getParamFromPathVariable(Matcher matcher, Param param) {
            try {
                String s = matcher.group(param.name);
                return convertToType(param.classType, s);
            } catch (IllegalArgumentException e) {
                throw new ServerWebInputException("Path variable '" + param.name + "' not found.");
            }
        }

        /**
         * 处理@RequestBody
         *
         * @param request 客户端发送的HTTP请求对象
         * @param param   参数
         * @return 处理后的对象
         * @throws IOException 如果在处理请求和响应时发生I/O异常
         */
        private Object getParamFromRequestBody(HttpServletRequest request, Param param) throws IOException {
            BufferedReader reader = request.getReader();
            return JsonUtils.readJson(reader, param.classType);
        }

        /**
         * 处理@RequestParam
         *
         * @param request 客户端发送的HTTP请求对象
         * @param param   参数
         * @return 处理后的对象
         */
        private Object getParamFromRequestParam(HttpServletRequest request, Param param) {
            String s = getOrDefault(request, param.name, param.defaultValue);
            return convertToType(param.classType, s);
        }

        /**
         * 处理Servlet提供参数
         *
         * @param request  客户端发送的HTTP请求对象
         * @param response 服务端发送的HTTP响应对象
         * @param param    参数
         * @return 处理后的对象
         */
        private Object getParamFromServletVariable(HttpServletRequest request, HttpServletResponse response, Param param) {
            Class<?> classType = param.classType;
            if (classType == HttpServletRequest.class) {
                return request;
            } else if (classType == HttpServletResponse.class) {
                return response;
            } else if (classType == HttpSession.class) {
                return request.getSession();
            } else if (classType == ServletContext.class) {
                return request.getServletContext();
            } else {
                throw new ServerErrorException("Could not determine argument type: " + classType);
            }
        }

        /**
         * 将字符串转换为指定的类型
         *
         * @param classType 指定的类型
         * @param s         字符串
         * @return 指定类型的对象
         */
        private Object convertToType(Class<?> classType, String s) {
            if (classType == String.class) {
                return s;
            } else if (classType == boolean.class || classType == Boolean.class) {
                return Boolean.valueOf(s);
            } else if (classType == int.class || classType == Integer.class) {
                return Integer.valueOf(s);
            } else if (classType == long.class || classType == Long.class) {
                return Long.valueOf(s);
            } else if (classType == byte.class || classType == Byte.class) {
                return Byte.valueOf(s);
            } else if (classType == short.class || classType == Short.class) {
                return Short.valueOf(s);
            } else if (classType == float.class || classType == Float.class) {
                return Float.valueOf(s);
            } else if (classType == double.class || classType == Double.class) {
                return Double.valueOf(s);
            } else {
                throw new ServerErrorException("Could not determine argument type: " + classType);
            }
        }

        /**
         * 从HttpServletRequest对象中获取指定名称的请求参数值，若参数不存在，则返回默认值
         *
         * @param request      客户端发送的HTTP请求对象
         * @param name         请求参数名称
         * @param defaultValue 默认值
         * @return 请求参数的值，或者默认值
         */
        private String getOrDefault(HttpServletRequest request, String name, String defaultValue) {
            // 从HttpServletRequest对象中获取指定名称的请求参数值
            String s = request.getParameter(name);
            // 如果请求中不存在该参数
            if (s == null) {
                // 如果未指定默认值，则抛出异常
                if (WebUtils.DEFAULT_PARAM_VALUE.equals(defaultValue)) {
                    throw new ServerWebInputException("Request parameter '" + name + "' not found.");
                }
                // 返回默认值
                return defaultValue;
            }
            // 返回请求参数值
            return s;
        }
    }

    /**
     * 参数类
     */
    static class Param {
        /**
         * 参数名称
         */
        String name;
        /**
         * 参数类型，分为路径参数、URL参数、REST请求参数、Servlet提供参数
         */
        ParamType paramType;
        /**
         * 参数Class类型
         */
        Class<?> classType;
        /**
         * 参数默认值
         */
        String defaultValue;

        /**
         * 创建一个Param对象，并根据方法的参数注解类型设置参数的类型和名称
         *
         * @param handlerMethod 控制器方法
         * @param parameter     方法的参数对象
         * @param annotations   方法的参数上的注解数组
         * @throws ServletException 如果在处理请求和响应时发生I/O异常
         */
        public Param(Method handlerMethod, Parameter parameter, Annotation[] annotations) throws ServletException {
            // 获取参数的各个注解
            PathVariable pv = ClassUtils.getAnnotation(annotations, PathVariable.class);
            RequestParam rp = ClassUtils.getAnnotation(annotations, RequestParam.class);
            RequestBody rb = ClassUtils.getAnnotation(annotations, RequestBody.class);
            // 检查是否只为一个注解
            int total = (pv == null ? 0 : 1) + (rp == null ? 0 : 1) + (rb == null ? 0 : 1);
            if (total > 1) {
                throw new ServletException("Annotation @PathVariable, @RequestParam and @RequestBody cannot be combined at method: " + handlerMethod);
            }
            // 参数Class类型
            this.classType = parameter.getType();
            // 根据注解类型设置参数类型
            if (pv != null) {
                // @PathVariable
                this.paramType = ParamType.PATH_VARIABLE;

                this.name = pv.value();
            } else if (rp != null) {
                // @RequestParam
                this.paramType = ParamType.REQUEST_PARAM;

                this.name = rp.value();
                this.defaultValue = rp.defaultValue();
            } else if (rb != null) {
                // @RequestBody
                this.paramType = ParamType.REQUEST_BODY;
            } else {
                this.paramType = ParamType.SERVLET_VARIABLE;

                // 检查是否servlet提供的API
                if (this.classType != HttpServletRequest.class &&
                        this.classType != HttpServletResponse.class &&
                        this.classType != HttpSession.class &&
                        this.classType != ServletContext.class
                ) {
                    throw new ServerErrorException("(Missing annotation?) Unsupported argument type: " + this.classType + " at method: " + handlerMethod);
                }
            }
        }

        /**
         * 返回Param对象的字符串表示形式
         *
         * @return Param对象的字符串表示形式
         */
        @Override
        public String toString() {
            return "Param [name=" + name + ", paramType=" + paramType + ", classType=" + classType + ", defaultValue=" + defaultValue + "]";
        }
    }

    /**
     * 参数类型枚举类
     */
    enum ParamType {
        /**
         * 路径参数，从URL中提取
         */
        PATH_VARIABLE,
        /**
         * URL参数，从URL Query或Form表单提取
         */
        REQUEST_PARAM,
        /**
         * REST请求参数，从Post传递的JSON提取
         */
        REQUEST_BODY,
        /**
         * HttpServletRequest等Servlet API提供的参数，直接从DispatcherServlet的方法参数获得
         */
        SERVLET_VARIABLE;
    }

    /**
     * 结果类
     *
     * @param processed    是否已被处理
     * @param returnObject 返回对象
     */
    record Result(boolean processed, Object returnObject) {
    }
}
