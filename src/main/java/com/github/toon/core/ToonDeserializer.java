package com.github.toon.core;

import com.github.toon.exception.ToonException;

public interface ToonDeserializer {
    <T> T deserialize(String toonStr, Class<T> targetType) throws ToonException;
}
