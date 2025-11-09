package com.github.toon.core;

import com.github.toon.exception.ToonException;

public interface ToonSerializer {
    String serialize(String rootName, Object data) throws ToonException;
}


