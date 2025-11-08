package com.github.toon.exception;

public class ToonTypeConvertException extends ToonException {
    private final String fieldName;
    private final Class<?> targetType;
    public ToonTypeConvertException(String fieldName, Class<?> targetType, String value) {
        super(String.format("字段[%s]无法转换为类型[%s]，值：%s", fieldName, targetType.getName(), value));
        this.fieldName = fieldName;
        this.targetType = targetType;
    }
}
