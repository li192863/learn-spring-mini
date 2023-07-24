package com.chestnut.spring.jdbc.transaction;

import com.chestnut.spring.annotation.Transactional;
import com.chestnut.spring.aop.AnnotationProxyBeanPostProcessor;

/**
 * 事务Bean后处理器
 *
 * @author: Chestnut
 * @since: 2023-07-20
 **/
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {
}
