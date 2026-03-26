package com.example.cae.gateway.filter;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.dto.UserContextDTO;
import com.example.cae.common.utils.JwtUtil;

public class JwtAuthFilter {
    public boolean validate(String token) {
        return JwtUtil.validateToken(token);
    }

    public UserContextDTO parse(String token) {
        return JwtUtil.parseUserContext(token);
    }

    public String userIdHeader() {
        return HeaderConstants.X_USER_ID;
    }
}
