package com.github.toon.anno;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 标记TOON序列化的字段及顺序
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToonField {
    int order(); // 字段顺序（值越小越靠前）
    String comment() default ""; // 新增：字段注释，默认空
}