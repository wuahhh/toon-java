package com.github.toon.converter;

import java.util.ArrayList;
import java.util.List;

public class ConverterRegistry {
    private final List<TypeConverter> converters = new ArrayList<>();

    public ConverterRegistry() {
        // 注册内置转换器
        converters.add(new LocalDateTimeConverter());
        converters.add(new EnumConverter()); // 枚举转换器
        // 其他默认转换器...
    }

    public void addConverter(TypeConverter converter) {
        converters.add(0, converter); // 自定义转换器优先
    }

    public TypeConverter findConverter(Class<?> type) {
        for (TypeConverter converter : converters) {
            if (converter.support(type)) {
                return converter;
            }
        }
        return null; // 未找到则使用默认反射转换
    }
}
