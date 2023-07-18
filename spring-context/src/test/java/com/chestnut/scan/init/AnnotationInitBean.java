package com.chestnut.scan.init;

import com.chestnut.spring.annotation.Component;
import com.chestnut.spring.annotation.Value;

import jakarta.annotation.PostConstruct;

@Component
public class AnnotationInitBean {

    @Value("${app.title}")
    String appTitle;

    @Value("${app.version}")
    String appVersion;

    public String appName;

    @PostConstruct
    void init() {
        this.appName = this.appTitle + " / " + this.appVersion;
    }
}
