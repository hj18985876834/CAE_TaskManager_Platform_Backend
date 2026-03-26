package com.example.cae.common.utils;

public final class FileNameUtil {
    private FileNameUtil() {
    }

    public static String getSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
