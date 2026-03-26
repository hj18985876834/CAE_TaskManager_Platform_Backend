package com.example.cae.common.utils;

import com.example.cae.common.dto.UserContextDTO;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JwtUtil {
    private JwtUtil() {
    }

    public static String generateToken(Long userId, String roleCode) {
        String payload = userId + ":" + roleCode;
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static UserContextDTO parseUserContext(String token) {
        String text = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        String[] parts = text.split(":", 2);
        UserContextDTO dto = new UserContextDTO();
        dto.setUserId(Long.parseLong(parts[0]));
        dto.setRoleCode(parts.length > 1 ? parts[1] : "USER");
        return dto;
    }

    public static Long parseUserId(String token) {
        return parseUserContext(token).getUserId();
    }

    public static String parseRoleCode(String token) {
        return parseUserContext(token).getRoleCode();
    }

    public static boolean validateToken(String token) {
        try {
            parseUserContext(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
