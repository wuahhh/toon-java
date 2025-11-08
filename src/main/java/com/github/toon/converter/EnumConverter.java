package com.github.toon.converter;

import com.github.toon.exception.ToonTypeConvertException;

public class EnumConverter implements TypeConverter {
    @Override
    public boolean support(Class<?> type) {
        return type.isEnum();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convert(String value, Class<?> type) throws ToonTypeConvertException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf((Class<? extends Enum>) type, value.trim());
        } catch (IllegalArgumentException e) {
            throw new ToonTypeConvertException("", type, value);
        }
    }

    @Override
    public String serialize(Object value) {
        if (value == null) {
            return "";
        }
        return ((Enum<?>) value).name();
    }
}
