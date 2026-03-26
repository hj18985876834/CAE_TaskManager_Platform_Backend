package com.example.cae.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception ex) {
            throw new RuntimeException("json serialize failed", ex);
        }
    }

    public static <T> T fromJson(String text, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(text, clazz);
        } catch (Exception ex) {
            throw new RuntimeException("json deserialize failed", ex);
        }
    }
}
