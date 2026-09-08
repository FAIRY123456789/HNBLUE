package com.example.jpaspringboot.util;

public final class PasswordPolicy {
    public static final String MESSAGE = "密码至少 8 位，且需同时包含英文字母和数字。";
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        if (password == null) return false;
        String value = password.trim();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isISOControl(ch) || Character.isWhitespace(ch)) return false;
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) hasLetter = true;
            else if (ch >= '0' && ch <= '9') hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    public static void requireValid(String password) {
        if (!isValid(password)) throw new IllegalArgumentException(MESSAGE);
    }
}
