package com.example.cae.gateway.filter;

import java.util.UUID;

public class TraceIdFilter {
    public String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
