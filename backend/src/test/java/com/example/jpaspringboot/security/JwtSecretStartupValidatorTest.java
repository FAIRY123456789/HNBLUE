package com.example.jpaspringboot.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretStartupValidatorTest {
    @Test
    void permitsEphemeralKeyOnlyWhenGuardIsDisabled() {
        assertDoesNotThrow(() -> JwtSecretStartupValidator.validate(false, null, null));
    }

    @Test
    void rejectsMissingProductionSecret() {
        assertThrows(IllegalStateException.class,
                () -> JwtSecretStartupValidator.validate(true, null, "  "));
    }

    @Test
    void rejectsShortProductionSecret() {
        assertThrows(IllegalStateException.class,
                () -> JwtSecretStartupValidator.validate(true, "too-short", null));
    }

    @Test
    void acceptsConfiguredProductionSecret() {
        assertDoesNotThrow(() -> JwtSecretStartupValidator.validate(true,
                "0123456789abcdef0123456789abcdef", null));
    }
}
