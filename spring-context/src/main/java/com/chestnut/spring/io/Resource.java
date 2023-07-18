package com.chestnut.spring.io;

/**
 * 文件（record类：类名处声明字段/构造方法，字段默认private final，默认生成equals/hashCode/toString方法）
 *
 * @author: Chestnut
 * @since: 2023-07-11
 **/
public record Resource(String path, String name) {
}
