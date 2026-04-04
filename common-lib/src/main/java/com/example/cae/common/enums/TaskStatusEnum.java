package com.example.cae.common.enums;

public enum TaskStatusEnum {
    CREATED,
    VALIDATED,
    QUEUED,
    SCHEDULED,
    DISPATCHED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELED,
    TIMEOUT;

    public boolean isFinished() {
        return this == SUCCESS || this == FAILED || this == CANCELED || this == TIMEOUT;
    }

    public boolean canCancel() {
        return this == QUEUED || this == RUNNING;
    }
}
