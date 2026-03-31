package com.example.cae.common.constant;

public final class ErrorCodeConstants {
    private ErrorCodeConstants() {
    }

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int BAD_GATEWAY = 502;

    public static final int TASK_VALIDATION_FAILED = 4001;
    public static final int TASK_STATUS_NOT_EDITABLE = 4002;
    public static final int TASK_NOT_VALIDATED = 4003;
    public static final int TASK_PROFILE_MISMATCH = 4004;
    public static final int TASK_TYPE_MISMATCH = 4005;
    public static final int TASK_CANCEL_NOT_ALLOWED = 4006;
    public static final int TASK_STATUS_MISMATCH = 4007;
    public static final int TASK_STATUS_TRANSFER_ILLEGAL = 4008;
    public static final int TASK_STATUS_UNSUPPORTED = 4009;
    public static final int INVALID_SCHEDULE_TASK = 4010;
    public static final int INVALID_NODE_STATUS_REQUEST = 4011;
    public static final int UNSUPPORTED_NODE_STATUS = 4012;

    public static final int INVALID_LOGIN_CREDENTIALS = 4101;
    public static final int USER_DISABLED = 4102;
    public static final int USERNAME_ALREADY_EXISTS = 4103;
    public static final int SOLVER_CODE_ALREADY_EXISTS = 4201;
    public static final int PROFILE_CODE_ALREADY_EXISTS = 4202;
    public static final int PROFILE_DISABLED = 4203;
    public static final int FILE_RULE_CONFLICT = 4204;
    public static final int SOLVER_DISABLED = 4205;
    public static final int NODE_TOKEN_REQUIRED = 4301;
    public static final int INVALID_NODE_TOKEN = 4302;
    public static final int REPORTED_NODE_MISMATCH = 4303;

    public static final int TASK_NOT_FOUND = 4401;
    public static final int PROFILE_NOT_FOUND = 4402;
    public static final int SOLVER_NOT_FOUND = 4403;
    public static final int USER_NOT_FOUND = 4404;
    public static final int ROLE_NOT_FOUND = 4405;
    public static final int NODE_NOT_FOUND = 4406;
    public static final int RESULT_FILE_NOT_FOUND = 4407;
    public static final int FILE_RULE_NOT_FOUND = 4408;

    public static final int TASK_NOT_BOUND_TO_NODE = 4501;
    public static final int NO_AVAILABLE_NODE = 4502;
    public static final int NODE_AGENT_REJECTED = 4503;
    public static final int NODE_AGENT_EMPTY_RESPONSE = 5501;
}
