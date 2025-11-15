package com.github.toon.core;

public class ToonFactory {
    private static ToonSerializer defaultSerializer = new DefaultToonSerializer();

    public static ToonSerializer getSerializer() { return defaultSerializer; }

    // 支持自定义实现注入
    public static void setSerializer(ToonSerializer serializer) { defaultSerializer = serializer; }
}
