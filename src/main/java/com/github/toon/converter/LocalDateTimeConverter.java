package com.github.toon.converter;

import com.github.toon.exception.ToonTypeConvertException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateTimeConverter implements  TypeConverter {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public boolean support(Class<?> type) {
        return LocalDateTime.class.equals(type);
    }

    @Override
    public Object convert(String value, Class<?> type) throws ToonTypeConvertException {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ToonTypeConvertException("", type, value);
        }
    }

    @Override
    public String serialize(Object value) {
        return value == null ? "" : FORMATTER.format((LocalDateTime) value);
    }
}
