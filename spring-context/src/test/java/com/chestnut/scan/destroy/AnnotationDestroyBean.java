package com.chestnut.scan.destroy;

import com.chestnut.spring.annotation.Component;
import com.chestnut.spring.annotation.Value;

import jakarta.annotation.PreDestroy;

@Component
public class AnnotationDestroyBean {

    @Value("${app.title}")
    public String appTitle;

    @PreDestroy
    void destroy() {
        this.appTitle = null;
    }
}
