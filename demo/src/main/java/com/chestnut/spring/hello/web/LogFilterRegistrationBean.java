package com.chestnut.spring.hello.web;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chestnut.spring.annotation.Component;
import com.chestnut.spring.annotation.Order;
import com.chestnut.spring.web.FilterRegistrationBean;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Order(100)
@Component
public class LogFilterRegistrationBean extends FilterRegistrationBean {

    @Override
    public List<String> getUrlPatterns() {
        return List.of("/*");
    }

    @Override
    public Filter getFilter() {
        return new LogFilter();
    }
}

class LogFilter implements Filter {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        logger.info("{}: {}", req.getMethod(), req.getRequestURI());
        chain.doFilter(request, response);
    }
}