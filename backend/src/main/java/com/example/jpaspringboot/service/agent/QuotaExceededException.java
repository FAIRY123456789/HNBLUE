package com.example.jpaspringboot.service.agent;

public class QuotaExceededException extends RuntimeException {
    private final String code;

    public QuotaExceededException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
