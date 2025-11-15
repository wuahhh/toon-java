package com.github.toon.core;

import com.github.toon.anno.ToonField;
import com.github.toon.converter.ConverterRegistry;
import com.github.toon.converter.TypeConverter;
import com.github.toon.exception.ToonException;
import com.github.toon.exception.ToonReflectionException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultToonSerializer implements ToonSerializer {
    private final ConverterRegistry converterRegistry;
    private static final String INDENT_CHAR = "  "; // 缩进字符（2个空格）

    public DefaultToonSerializer() {
        this.converterRegistry = new ConverterRegistry();
    }

    // 支持自定义转换器
    public void addConverter(TypeConverter converter) {
        converterRegistry.addConverter(converter);
    }

    @Override
    public String serialize(String rootName, Object data) throws ToonException {
        if (data == null) {
            return rootName + ": null";
        }

        StringBuilder builder = new StringBuilder();
        // 处理集合类型（List/Set）
        if (data instanceof Collection<?>) {
            serializeCollection(rootName, (Collection<?>) data, builder, 0);
        } else {
            // 处理单个对象（含嵌套）
            serializeObject(rootName, data, builder, 0);
        }
        return builder.toString().trim(); // 去除首尾空白
    }

    // 序列化集合（如List<User>）
    private void serializeCollection(String collectionName, Collection<?> collection, StringBuilder builder, int indent) throws ToonException {
        if (collection.isEmpty()) {
            builder.append(getIndent(indent)).append(collectionName).append("(0){}: ");
            return;
        }

        Object firstElement = collection.iterator().next();
        Class<?> elementType = firstElement.getClass();

        // 构建包含嵌套结构的完整元数据
        List<String> nestedMetas = buildNestedFieldMetas(elementType, "");
        String fieldsStr = String.join(",", nestedMetas);

        builder.append(getIndent(indent))
                .append(collectionName)
                .append("(").append(collection.size()).append(")")
                .append("{").append(fieldsStr).append("}: \n");

        // 序列化集合元素（使用紧凑格式）
        int elementIndent = indent + 1;
        for (Object element : collection) {
            serializeCompressedElement(element, elementType, builder, elementIndent);
        }
    }

    // 新增：序列化压缩格式的元素值
    private void serializeCompressedElement(Object element, Class<?> type, StringBuilder builder, int indent) throws ToonException {
        List<String> valueList = new ArrayList<>();
        collectNestedValues(element, type, "", valueList);
        builder.append(getIndent(indent))
                .append(String.join(",", valueList))
                .append(";\n");
    }

    /**
     * 序列化嵌套在对象中的集合类型字段值
     * 生成格式：(元素1,元素2,...) （用于嵌套场景的紧凑格式）
     */
    private String serializeCollectionValue(Collection<?> collection) throws ToonException {
        if (collection == null || collection.isEmpty()) {
            return "()"; // 空集合标识
        }

        List<String> elementValues = new ArrayList<>();
        for (Object element : collection) {
            if (element == null) {
                elementValues.add("");
                continue;
            }

            Class<?> elementType = element.getClass();
            // 基础类型或支持的类型直接序列化值
            if (isPrimitiveOrSupported(elementType)) {
                elementValues.add(serializePrimitiveValue(element, elementType));
            }
            // 嵌套对象递归处理（用括号包裹）
            else if (!Collection.class.isAssignableFrom(elementType)) {
                List<String> nestedValues = new ArrayList<>();
                collectNestedValues(element, elementType, "", nestedValues);
                elementValues.add("(" + String.join(",", nestedValues) + ")");
            }
            // 嵌套集合（多层集合）
            else {
                elementValues.add(serializeCollectionValue((Collection<?>) element));
            }
        }

        return "(" + String.join(",", elementValues) + ")";
    }

    // 新增：递归收集嵌套对象的值
    private void collectNestedValues(Object obj, Class<?> type, String parentPath, List<String> valueList) throws ToonException {
        List<Field> fields = FieldCache.getOrderedFields(type);
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                Class<?> fieldType = field.getType();

                if (value == null) {
                    valueList.add("");
                    continue;
                }

                // 基础类型直接添加值
                if (isPrimitiveOrSupported(fieldType)) {
                    valueList.add(serializePrimitiveValue(value, fieldType));
                }
                // 嵌套对象用括号包裹值序列
                else if (!Collection.class.isAssignableFrom(fieldType)) {
                    List<String> nestedValues = new ArrayList<>();
                    collectNestedValues(value, fieldType, parentPath + field.getName() + ".", nestedValues);
                    valueList.add("(" + String.join(",", nestedValues) + ")");
                }
                // 集合类型保持原有格式
                else {
                    // 集合处理逻辑保持不变
                    valueList.add(serializeCollectionValue((Collection<?>) value));
                }
            } catch (IllegalAccessException e) {
                throw new ToonReflectionException(type, "无法访问字段[" + field.getName() + "]", e);
            }
        }
    }



    private List<String> buildNestedFieldMetas(Class<?> type, String parentPath) throws ToonException {
        List<String> metas = new ArrayList<>();
        List<Field> fields = FieldCache.getOrderedFields(type);

        for (Field field : fields) {
            ToonField toonField = field.getAnnotation(ToonField.class);
            String fieldName = field.getName();
            String fullPath = parentPath.isEmpty() ? fieldName : parentPath + "." + fieldName;
            String comment = toonField.comment();

            Class<?> fieldType = field.getType();
            // 判断是否为嵌套对象（非基础类型且非集合）
            if (!isPrimitiveOrSupported(fieldType) && !Collection.class.isAssignableFrom(fieldType)) {
                // 递归处理嵌套对象，添加类型标记
                metas.add(fullPath + "#" + comment + "[$object]");
                metas.addAll(buildNestedFieldMetas(fieldType, fullPath));
            } else {
                metas.add(fullPath + "#" + comment);
            }
        }
        return metas;
    }

    // 序列化集合中的单个元素（表格化数据）
    private void serializeCollectionElement(Object element, List<Field> fields, StringBuilder builder, int indent) throws ToonException {
        List<String> valueList = new ArrayList<>();
        for (Field field : fields) {
            valueList.add(serializeFieldValue(element, field));
        }
        builder.append(getIndent(indent))
                .append(String.join(",", valueList))
                .append(";\n");
    }

    // 序列化单个对象（支持嵌套）
    private void serializeObject(String objectName, Object object, StringBuilder builder, int indent) throws ToonException {
        Class<?> clazz = object.getClass();
        List<Field> fields = FieldCache.getOrderedFields(clazz);

        // 构建对象元数据（字段声明）
        String fieldsStr = fields.stream()
                .map(field -> {
                    ToonField annotation = field.getAnnotation(ToonField.class);
                    String comment = annotation.comment();
                    return comment.isEmpty() ? field.getName() : field.getName() + "#" + comment;
                })
                .collect(Collectors.joining(","));
        builder.append(getIndent(indent))
                .append(objectName)
                .append("{").append(fieldsStr).append("}: \n");

        // 序列化字段（缩进+1）
        int fieldIndent = indent + 1;
        for (Field field : fields) {
            serializeField(object, field, builder, fieldIndent);
        }
    }

    // 序列化单个字段（处理基础类型、集合、嵌套对象）
    private void serializeField(Object parent, Field field, StringBuilder builder, int indent) throws ToonException {
        try {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object value = field.get(parent);

            if (value == null) {
                builder.append(getIndent(indent)).append(fieldName).append(": null\n");
                return;
            }

            Class<?> fieldType = field.getType();
            // 1. 基础类型或有转换器的类型
            if (isPrimitiveOrSupported(fieldType)) {
                String serializedValue = serializePrimitiveValue(value, fieldType);
                builder.append(getIndent(indent))
                        .append(fieldName)
                        .append(": ")
                        .append(serializedValue)
                        .append("\n");
            }
            // 2. 集合类型
            else if (Collection.class.isAssignableFrom(fieldType)) {
                serializeCollection(fieldName, (Collection<?>) value, builder, indent);
            }
            // 3. 嵌套对象
            else {
                serializeObject(fieldName, value, builder, indent);
            }
        } catch (IllegalAccessException e) {
            throw new ToonReflectionException(parent.getClass(), "无法访问字段[" + field.getName() + "]", e);
        }
    }

    // 序列化基础类型值（含转换器支持的类型）
    private String serializePrimitiveValue(Object value, Class<?> type) {
        TypeConverter converter = converterRegistry.findConverter(type);
        if (converter != null) {
            return escape(converter.serialize(value));
        }
        // 处理默认基本类型
        return escape(value.toString());
    }

    // 序列化集合元素中的字段值（简化版，用于表格化数据）
    private String serializeFieldValue(Object element, Field field) throws ToonException {
        try {
            field.setAccessible(true);
            Object value = field.get(element);
            if (value == null) {
                return "";
            }
            Class<?> type = field.getType();
            TypeConverter converter = converterRegistry.findConverter(type);
            return converter != null ? escape(converter.serialize(value)) : escape(value.toString());
        } catch (IllegalAccessException e) {
            throw new ToonReflectionException(element.getClass(), "无法访问字段[" + field.getName() + "]", e);
        }
    }

    // 工具方法：生成缩进字符串
    private String getIndent(int level) {
        if (level <= 0) {
            return "";
        }
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append(INDENT_CHAR);
        }
        return indent.toString();
    }

    // 工具方法：判断是否为基础类型或有转换器支持的类型
    private boolean isPrimitiveOrSupported(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Boolean.class
                || converterRegistry.findConverter(type) != null;
    }

    // 工具方法：特殊字符转义
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("\n", "\\n");
    }
}
