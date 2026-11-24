package com.example.jpaspringboot.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Prevent production-like profiles from silently generating an ephemeral JWT key. */
@Component
public class JwtSecretStartupValidator {
    private final Environment environment;
    private final boolean required;

    public JwtSecretStartupValidator(Environment environment,
                                     @Value("${hnblue.jwt.require-configured:false}") boolean required) {
        this.environment = environment;
        this.required = required;
    }

    @PostConstruct
    void validateAtStartup() {
        validate(required, environment.getProperty("HNBLUE_JWT_SECRET"),
                environment.getProperty("hnblue.jwt.secret"));
    }

    static void validate(boolean required, String environmentSecret, String systemSecret) {
        if (!required) return;
        String configured = firstNonBlank(environmentSecret, systemSecret);
        if (configured == null) {
            throw new IllegalStateException(
                    "HNBLUE_JWT_SECRET is required when hnblue.jwt.require-configured=true");
        }
        if (configured.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("HNBLUE_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first.trim();
        if (second != null && !second.isBlank()) return second.trim();
        return null;
    }
}
