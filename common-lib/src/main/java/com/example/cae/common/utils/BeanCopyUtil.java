package com.example.cae.common.utils;

import org.springframework.beans.BeanUtils;

public final class BeanCopyUtil {
    private BeanCopyUtil() {
    }

    public static <T> T copy(Object source, Class<T> targetClass) {
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception ex) {
            throw new RuntimeException("bean copy failed", ex);
        }
    }
}
