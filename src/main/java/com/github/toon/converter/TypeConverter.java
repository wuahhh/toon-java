package com.github.toon.converter;

import com.github.toon.exception.ToonTypeConvertException;

public interface TypeConverter {
    boolean support(Class<?> type);
    Object convert(String value, Class<?> type) throws ToonTypeConvertException;
    String serialize(Object value);
}
