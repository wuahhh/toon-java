package com.github.toon.exception;

public class ToonReflectionException extends ToonException {
    private final Class<?> targetClass;
    public ToonReflectionException(Class<?> targetClass, String message, Throwable cause) {
        super(String.format("类[%s]反射操作失败：%s", targetClass.getName(), message), cause);
        this.targetClass = targetClass;
    }
}
