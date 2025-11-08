package com.github.toon;

import com.github.toon.anno.ToonField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FieldCache {
    private static final ConcurrentHashMap<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    // 获取按@ToonField排序的字段列表（缓存）
    public static List<Field> getOrderedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, key -> {
            List<Field> fields = new ArrayList<>();
            // 递归获取父类字段（支持继承）
            Class<?> current = key;
            while (current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(ToonField.class)) {
                        fields.add(field);
                    }
                }
                current = current.getSuperclass();
            }
            // 按order排序
            fields.sort(Comparator.comparingInt(f -> f.getAnnotation(ToonField.class).order()));
            return fields;
        });
    }
}
