package com.chestnut.spring.web;

import com.chestnut.spring.annotation.PathVariable;
import com.chestnut.spring.annotation.RequestBody;
import com.chestnut.spring.annotation.RequestParam;
import com.chestnut.spring.annotation.ResponseBody;
import com.chestnut.spring.exception.ClientErrorException;
import com.chestnut.spring.exception.ServerErrorException;
import com.chestnut.spring.utils.ClassUtils;
import com.chestnut.spring.web.utils.JsonUtils;
import com.chestnut.spring.web.utils.PathUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理特定URL的处理器
 *
 * @author: Chestnut
 * @since: 2023-07-24
 **/
public class Dispatcher {
    /**
     * 日志记录器
     */
    final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 未处理结果
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
     * @param urlMapping    URL路径
     * @throws ServletException 如果在Servlet处理过程中发生异常
     */
    public Dispatcher(boolean isRest, Object controller, Method handlerMethod, String urlMapping) throws ServletException {
        this.isRest = isRest;
        this.isResponseBody = handlerMethod.getAnnotation(ResponseBody.class) != null;
        this.isVoid = handlerMethod.getReturnType() == void.class || handlerMethod.getReturnType() == Void.class;
        this.urlPattern = PathUtils.compile(urlMapping);
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
        logger.atDebug().log("mapping {} to handler {}.{}", urlMapping, controller.getClass().getSimpleName(), handlerMethod.getName());
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
     * @param uri      URL路径
     * @param request  客户端发送的HTTP请求对象
     * @param response 服务端发送的HTTP响应对象
     * @return 处理结果对象
     * @throws Exception 如果处理过程中发生异常
     */
    public Result process(String uri, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 使用urlPattern正则表达式匹配传入的URL
        Matcher matcher = this.urlPattern.matcher(uri);
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
                // 可能抛出ClientErrorException
                case PATH_VARIABLE -> getParamFromPathVariable(matcher, param);
                // 可能抛出IOException
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
            // 抛出原始异常
            if (e.getCause() instanceof Exception ex) {
                throw ex;
            }
            throw e;
        } catch (ReflectiveOperationException e) {
            // 将该异常转化为ServerErrorException，在DispatcherServlet中被处理
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
            // 将该异常转化为ClientErrorException，在DispatcherServlet中被处理
            throw new ClientErrorException("Path variable '" + param.name + "' not found.");
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
        String s = request.getParameter(param.name);
        // 如果请求中不存在该参数
        if (s == null) {
            // 如果未指定默认值，则抛出异常
            if (WebMvcConfiguration.DEFAULT_PARAM_VALUE.equals(param.defaultValue)) {
                // 将该异常转化为ClientErrorException，在DispatcherServlet中被处理
                throw new ClientErrorException("Request parameter '" + param.name + "' not found.");
            }
            s = param.defaultValue;
        }
        // 返回请求参数值
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
            // 将该异常转化为ServerErrorException，在DispatcherServlet中被处理
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
            // 将该异常转化为ServerErrorException，在DispatcherServlet中被处理
            throw new ServerErrorException("Could not determine argument type: " + classType);
        }
    }
}

/**
 * 参数类
 */
class Param {
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
                // 将该异常转化为ServerErrorException，在DispatcherServlet中被处理
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
