package com.example.cae.common.utils;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {
    }

    public static String nextTaskNo() {
        return "TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    public static String nextBizId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
