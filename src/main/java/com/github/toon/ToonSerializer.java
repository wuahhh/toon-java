package com.github.toon;

import com.github.toon.exception.ToonException;

public interface ToonSerializer {
    String serialize(String rootName, Object data) throws ToonException;
}


