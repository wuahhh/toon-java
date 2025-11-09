package com.github.toon.core;

public class ToonFactory {
    private static ToonSerializer defaultSerializer = new DefaultToonSerializer();
    private static ToonDeserializer defaultDeserializer = new DefaultToonDeserializer();

    public static ToonSerializer getSerializer() { return defaultSerializer; }
    public static ToonDeserializer getDeserializer() { return defaultDeserializer; }

    // 支持自定义实现注入
    public static void setSerializer(ToonSerializer serializer) { defaultSerializer = serializer; }
    public static void setDeserializer(ToonDeserializer deserializer) { defaultDeserializer = deserializer; }
}
