package com.github.toon;

import com.github.toon.core.DefaultToonSerializer;
import com.github.toon.exception.ToonException;

public class Toons {
    private Toons() {}
    // 全局单例，避免重复初始化
    private static final DefaultToonSerializer SERIALIZER = new DefaultToonSerializer();

    // 静态代码块：注册全局转换器
    static {
        //SERIALIZER.addConverter(new BigDecimalConverter());
        //DESERIALIZER.addConverter(new BigDecimalConverter());
    }

    // 序列化工具方法
    public static String serialize(String rootName, Object data) throws ToonException {
        return SERIALIZER.serialize(rootName, data);
    }

}
