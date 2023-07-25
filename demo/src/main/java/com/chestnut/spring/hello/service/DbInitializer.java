package com.chestnut.spring.hello.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chestnut.spring.annotation.Autowired;
import com.chestnut.spring.annotation.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DbInitializer {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    UserService userService;

    @PostConstruct
    void init() {
        logger.info("init database...");
        userService.initDb();
    }
}
