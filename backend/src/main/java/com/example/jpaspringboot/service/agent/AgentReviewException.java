package com.example.jpaspringboot.service.agent;

public class AgentReviewException extends RuntimeException {
    private final String code;

    public AgentReviewException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AgentReviewException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() { return code; }
}
