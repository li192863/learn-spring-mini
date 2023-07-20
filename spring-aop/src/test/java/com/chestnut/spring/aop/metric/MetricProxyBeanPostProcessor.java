package com.chestnut.spring.aop.metric;

import com.chestnut.spring.annotation.Component;
import com.chestnut.spring.aop.AnnotationProxyBeanPostProcessor;

@Component
public class MetricProxyBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Metric> {

}
