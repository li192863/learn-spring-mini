package com.chestnut.spring.web.controller;

import com.chestnut.spring.annotation.Configuration;
import com.chestnut.spring.annotation.Import;
import com.chestnut.spring.web.WebMvcConfiguration;

@Configuration
@Import(WebMvcConfiguration.class)
public class ControllerConfiguration {

}
