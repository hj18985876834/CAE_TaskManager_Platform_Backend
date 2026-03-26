package com.example.cae.user.domain.enums;

public enum UserStatusEnum {
    DISABLED(0),
    ENABLED(1);

    private final int code;

    UserStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
