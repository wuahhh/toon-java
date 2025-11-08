package com.github.toon.exception;

public class ToonException extends Exception {
    public ToonException(String message) { super(message); }
    public ToonException(String message, Throwable cause) { super(message, cause); }
}
