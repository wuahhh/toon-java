package com.github.toon.core;

import com.github.toon.converter.ConverterRegistry;
import com.github.toon.converter.TypeConverter;
import com.github.toon.exception.ToonException;
import com.github.toon.exception.ToonFormatException;
import com.github.toon.exception.ToonReflectionException;
import com.github.toon.exception.ToonTypeConvertException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DefaultToonDeserializer implements ToonDeserializer {
    private final ConverterRegistry converterRegistry;
    private static final Pattern COLLECTION_PATTERN = Pattern.compile("(\\w+)\\((\\d+)\\)\\{([^}]+)\\}:");
    private static final Pattern OBJECT_PATTERN = Pattern.compile("(\\w+)\\{([^}]+)\\}:");
    private static final String INDENT_CHAR = "  "; // 与序列化器保持一致的缩进字符

    public DefaultToonDeserializer() {
        this.converterRegistry = new ConverterRegistry();
    }

    // 支持添加自定义类型转换器
    public void addConverter(TypeConverter converter) {
        converterRegistry.addConverter(converter);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String toonStr, Class<T> targetType) throws ToonException {
        if (toonStr == null || toonStr.trim().isEmpty()) {
            return null;
        }

        // 预处理：去除首尾空白，按行分割（保留缩进信息）
        String[] lines = toonStr.trim().split("\n");
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim(); // 先trim掉每行首尾空白（但保留缩进导致的行内空白）
        }

        // 解析根节点
        return (T) parseNode(lines, 0, targetType, null);
    }

    // 递归解析节点（集合/对象/基础类型）
    private Object parseNode(String[] lines, int startLine, Class<?> targetType, Type genericType) throws ToonException {
        String firstLine = lines[startLine];
        // 1. 判断是否为集合（匹配collection(size){fields}:格式）
        Matcher collectionMatcher = COLLECTION_PATTERN.matcher(firstLine);
        if (collectionMatcher.matches()) {
            return parseCollection(lines, startLine, collectionMatcher, targetType, genericType);
        }

        // 2. 判断是否为对象（匹配object{fields}:格式）
        Matcher objectMatcher = OBJECT_PATTERN.matcher(firstLine);
        if (objectMatcher.matches()) {
            return parseObject(lines, startLine, objectMatcher, targetType);
        }

        // 3. 基础类型（直接返回值）
        return parsePrimitive(firstLine, targetType);
    }

    // 新增工具方法：从带注释的字段名中提取核心字段名（如"id#用户ID" -> "id"）
    private String extractFieldName(String fieldNameWithComment) {
        int hashIndex = fieldNameWithComment.indexOf('#');
        return hashIndex == -1 ? fieldNameWithComment : fieldNameWithComment.substring(0, hashIndex);
    }

    // 解析集合（如List<User>）
    private Object parseCollection(String[] lines, int startLine, Matcher matcher, Class<?> targetType, Type genericType) throws ToonException {
        String collectionName = matcher.group(1);
        int declaredSize = Integer.parseInt(matcher.group(2).trim());
        String fieldsStr = matcher.group(3).trim();

        List<String> fieldNames = fieldsStr.isEmpty() ? Collections.emptyList() :
                Arrays.stream(fieldsStr.split(","))
                        .map(this::extractFieldName) // 提取纯字段名
                        .collect(Collectors.toList());

        // 获取集合元素类型（如List<User>中的User）
        Class<?> elementType = getCollectionElementType(targetType, genericType);
        if (elementType == null) {
            throw new ToonFormatException("无法解析集合元素类型，目标类型：" + targetType.getName(), startLine);
        }

        // 解析集合元素行（从startLine+1开始，缩进层级为startLine的缩进+1）
        int elementIndent = getIndentLevel(lines[startLine]) + 1;
        List<Object> elements = new ArrayList<>();
        int currentLine = startLine + 1;

        while (currentLine < lines.length && getIndentLevel(lines[currentLine]) == elementIndent) {
            String elementLine = lines[currentLine].trim();
            if (elementLine.isEmpty()) {
                currentLine++;
                continue;
            }
            // 集合元素以分号结尾，去除分号
            if (elementLine.endsWith(";")) {
                elementLine = elementLine.substring(0, elementLine.length() - 1).trim();
            }
            // 解析单个元素（可能是基础类型或嵌套对象引用）
            Object element = parseCollectionElement(elementLine, elementType, fieldNames, lines, currentLine, elementIndent);
            elements.add(element);
            currentLine++;
        }

        // 校验声明大小与实际解析数量
        if (elements.size() != declaredSize) {
            throw new ToonFormatException(
                    String.format("集合[%s]声明大小[%d]与实际解析数量[%d]不符", collectionName, declaredSize, elements.size()),
                    startLine
            );
        }

        // 实例化集合（支持List/Set）
        if (List.class.isAssignableFrom(targetType)) {
            return elements;
        } else if (Set.class.isAssignableFrom(targetType)) {
            return new HashSet<>(elements);
        } else {
            throw new ToonFormatException("不支持的集合类型：" + targetType.getName(), startLine);
        }
    }

    // 解析集合中的单个元素
    private Object parseCollectionElement(String elementLine, Class<?> elementType, List<String> fieldNames, String[] lines, int currentLine, int elementIndent) throws ToonException {
        // 如果是基础类型（如List<String>）
        if (isPrimitiveOrSupported(elementType)) {
            return parsePrimitive(unescape(elementLine), elementType);
        }

        // 复杂对象：解析字段值映射
        String[] values = splitWithEscape(elementLine, ','); // 处理转义的逗号
        if (values.length != fieldNames.size()) {
            throw new ToonFormatException(
                    String.format("元素字段数量不匹配：声明[%d]，实际[%d]", fieldNames.size(), values.length),
                    currentLine
            );
        }

        // 构建字段-值映射（嵌套对象需要后续行解析）
        Map<String, Object> fieldData = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String valueStr = unescape(values[i].trim());
            // 嵌套对象的值可能为空（实际数据在后续缩进行）
            fieldData.put(fieldName, valueStr.isEmpty() ? null : valueStr);
        }

        // 解析嵌套在集合元素后的子对象（缩进层级为elementIndent+1）
        int nestedIndent = elementIndent + 1;
        int nestedLine = currentLine + 1;
        while (nestedLine < lines.length && getIndentLevel(lines[nestedLine]) == nestedIndent) {
            String nestedLineStr = lines[nestedLine].trim();
            if (nestedLineStr.isEmpty()) {
                nestedLine++;
                continue;
            }
            // 解析子对象节点（如address{...}:）
            Matcher nestedMatcher = OBJECT_PATTERN.matcher(nestedLineStr);
            if (nestedMatcher.matches()) {
                String nestedName = nestedMatcher.group(1);
                Class<?> nestedType = getFieldType(elementType, nestedName);
                Object nestedObj = parseNode(lines, nestedLine, nestedType, null);
                fieldData.put(nestedName, nestedObj);
                // 跳过子对象占用的行（递归解析时已处理）
                nestedLine += countNodeLines(lines, nestedLine);
            } else {
                nestedLine++;
            }
        }

        // 实例化对象并填充字段
        return instantiateObject(elementType, fieldData);
    }

    // 解析单个对象（如User）
    private Object parseObject(String[] lines, int startLine, Matcher matcher, Class<?> targetType) throws ToonException {
        String objectName = matcher.group(1);
        String fieldsStr = matcher.group(2).trim();
        List<String> fieldNames = fieldsStr.isEmpty() ? Collections.emptyList() :
                Arrays.stream(fieldsStr.split(","))
                        .map(this::extractFieldName) // 提取纯字段名
                        .collect(Collectors.toList());
        // 解析字段值（从startLine+1开始，缩进层级为startLine的缩进+1）
        int fieldIndent = getIndentLevel(lines[startLine]) + 1;
        Map<String, Object> fieldData = new HashMap<>();
        int currentLine = startLine + 1;

        while (currentLine < lines.length && getIndentLevel(lines[currentLine]) == fieldIndent) {
            String fieldLine = lines[currentLine].trim();
            if (fieldLine.isEmpty()) {
                currentLine++;
                continue;
            }

            // 分割字段名与值（如"name: Alice"）
            String[] fieldParts = splitFieldLine(fieldLine);
            if (fieldParts == null) {
                currentLine++;
                continue;
            }
            String fieldName = fieldParts[0].trim();
            String valueStr = fieldParts[1].trim();

            // 判断值是否为嵌套对象/集合
            if (COLLECTION_PATTERN.matcher(valueStr).matches() || OBJECT_PATTERN.matcher(valueStr).matches()) {
                // 递归解析嵌套节点
                Class<?> fieldType = getFieldType(targetType, fieldName);
                Type genericType = getFieldGenericType(targetType, fieldName);
                Object nestedValue = parseNode(lines, currentLine, fieldType, genericType);
                fieldData.put(fieldName, nestedValue);
                // 跳过嵌套节点占用的行
                currentLine += countNodeLines(lines, currentLine);
            } else {
                // 基础类型值
                fieldData.put(fieldName, unescape(valueStr));
                currentLine++;
            }
        }

        // 实例化对象并填充字段
        return instantiateObject(targetType, fieldData);
    }

    // 实例化对象并设置字段值
    private Object instantiateObject(Class<?> targetType, Map<String, Object> fieldData) throws ToonException {
        try {
            Object obj = targetType.newInstance(); // 要求目标类有默认构造函数
            List<Field> fields = FieldCache.getOrderedFields(targetType);

            for (Field field : fields) {
                String fieldName = field.getName();
                Object valueData = fieldData.get(fieldName);
                if (valueData == null) {
                    continue; // 跳过空值
                }

                Class<?> fieldType = field.getType();
                Object value = convertValue(fieldName, (String) valueData, fieldType);
                field.setAccessible(true);
                field.set(obj, value);
            }
            return obj;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ToonReflectionException(targetType, "实例化失败（需默认构造函数）", e);
        }
    }

    // 解析基础类型值
    private Object parsePrimitive(String valueStr, Class<?> targetType) throws ToonException {
        if (valueStr.equals("null")) {
            return null;
        }
        return convertValue("", valueStr, targetType);
    }

    // 转换值为目标类型（使用转换器或基础类型转换）
    private Object convertValue(String fieldName, String valueStr, Class<?> targetType) throws ToonException {
        if (valueStr == null || valueStr.isEmpty()) {
            return null;
        }

        // 尝试使用自定义转换器
        TypeConverter converter = converterRegistry.findConverter(targetType);
        if (converter != null) {
            return converter.convert(valueStr, targetType);
        }

        // 基础类型转换
        try {
            if (targetType == String.class) {
                return valueStr;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(valueStr);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(valueStr);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(valueStr);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(valueStr);
            } else {
                throw new ToonTypeConvertException(fieldName, targetType, valueStr);
            }
        } catch (NumberFormatException e) {
            throw new ToonTypeConvertException(fieldName, targetType, valueStr);
        }
    }

    // 工具方法：获取字段类型（用于嵌套对象解析）
    private Class<?> getFieldType(Class<?> parentType, String fieldName) throws ToonException {
        try {
            Field field = parentType.getDeclaredField(fieldName);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new ToonReflectionException(parentType, "字段[" + fieldName + "]不存在", e);
        }
    }

    // 工具方法：获取字段泛型类型（如List<User>中的User）
    private Type getFieldGenericType(Class<?> parentType, String fieldName) throws ToonException {
        try {
            Field field = parentType.getDeclaredField(fieldName);
            return field.getGenericType();
        } catch (NoSuchFieldException e) {
            throw new ToonReflectionException(parentType, "字段[" + fieldName + "]不存在", e);
        }
    }

    // 工具方法：获取集合元素类型（如List<User> -> User.class）
    private Class<?> getCollectionElementType(Class<?> collectionType, Type genericType) {
        if (genericType instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
            if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                return (Class<?>) actualTypeArguments[0];
            }
        }
        // 无法解析泛型时，默认使用Object
        return Object.class;
    }

    // 工具方法：计算节点占用的行数（用于跳过嵌套节点）
    private int countNodeLines(String[] lines, int startLine) {
        int indentLevel = getIndentLevel(lines[startLine]);
        int count = 1; // 至少包含自身行
        int currentLine = startLine + 1;
        while (currentLine < lines.length && getIndentLevel(lines[currentLine]) > indentLevel) {
            count++;
            currentLine++;
        }
        return count;
    }

    // 工具方法：获取行的缩进层级（基于INDENT_CHAR）
    private int getIndentLevel(String line) {
        int level = 0;
        int index = 0;
        while (index + INDENT_CHAR.length() <= line.length()) {
            if (line.substring(index, index + INDENT_CHAR.length()).equals(INDENT_CHAR)) {
                level++;
                index += INDENT_CHAR.length();
            } else {
                break;
            }
        }
        return level;
    }

    // 工具方法：分割字段行（如"name: Alice" -> ["name", "Alice"]）
    private String[] splitFieldLine(String line) {
        int colonIndex = findUnescapedIndex(line, ':');
        if (colonIndex == -1) {
            return null;
        }
        return new String[]{
                line.substring(0, colonIndex),
                line.substring(colonIndex + 1)
        };
    }

    // 工具方法：带转义的分割（如处理"a\,b" -> ["a,b"]）
    private String[] splitWithEscape(String str, char delimiter) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;

        for (char c : str.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == delimiter) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    // 工具方法：找到未被转义的字符索引
    private int findUnescapedIndex(String str, char target) {
        boolean escaped = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == target) {
                return i;
            }
        }
        return -1;
    }

    // 工具方法：还原特殊字符（与序列化器的escape对应）
    private String unescape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\{", "{")
                .replace("\\}", "}")
                .replace("\\\\", "\\")
                .replace("\\n", "\n");
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
}
