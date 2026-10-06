package com.example.jpaspringboot.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretCryptoServiceTest {
    private static final String TEST_MASTER_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void encryptsWithRandomNonceAndAuthenticatesSecretName() {
        SecretCryptoService crypto = new SecretCryptoService(TEST_MASTER_KEY);
        SecretCryptoService.EncryptedValue first = crypto.encrypt("deepseek_api_key", "unit-test-credential-1234567890");
        SecretCryptoService.EncryptedValue second = crypto.encrypt("deepseek_api_key", "unit-test-credential-1234567890");

        assertTrue(crypto.isReady());
        assertNotEquals(first.cipherText(), second.cipherText());
        assertNotEquals(first.initializationVector(), second.initializationVector());
        assertEquals("unit-test-credential-1234567890",
                crypto.decrypt("deepseek_api_key", first.cipherText(), first.initializationVector()));
        assertThrows(IllegalStateException.class,
                () -> crypto.decrypt("another_secret", first.cipherText(), first.initializationVector()));
    }

    @Test
    void refusesToEncryptWithoutServerMasterKey() {
        SecretCryptoService crypto = new SecretCryptoService("");
        assertFalse(crypto.isReady());
        assertThrows(IllegalStateException.class, () -> crypto.encrypt("deepseek_api_key", "unit-test-credential-1234567890"));
    }
}
