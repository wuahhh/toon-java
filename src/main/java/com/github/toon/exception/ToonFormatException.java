package com.github.toon.exception;

public class ToonFormatException extends ToonException {
    private final int position; // 错误位置（字符索引）
    public ToonFormatException(String message, int position) {
        super(String.format("格式错误（位置：%d）：%s", position, message));
        this.position = position;
    }
}
