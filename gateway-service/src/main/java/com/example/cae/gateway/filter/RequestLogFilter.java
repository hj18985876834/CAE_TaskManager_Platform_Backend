package com.example.cae.gateway.filter;

public class RequestLogFilter {
    public String buildAccessLog(String method, String path, long costMs) {
        return method + " " + path + " " + costMs + "ms";
    }
}
