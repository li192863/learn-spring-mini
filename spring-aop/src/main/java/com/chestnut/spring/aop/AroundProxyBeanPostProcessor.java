package com.chestnut.spring.aop;

import com.chestnut.spring.annotation.Around;

/**
 * 标注了@Around的代理处理类的后处理器
 *
 * @author: Chestnut
 * @since: 2023-07-19
 **/
public class AroundProxyBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Around> {
}
