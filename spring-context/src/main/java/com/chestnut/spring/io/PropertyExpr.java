package com.chestnut.spring.io;

/**
 * 属性表达式，包含属性键和默认值（可选）
 *
 * @author: Chestnut
 * @since: 2023-07-13
 **/
public record PropertyExpr(String key, String defaultValue) {
}
