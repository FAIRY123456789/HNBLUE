package com.example.jpaspringboot.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SecretCryptoService {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] masterKey;

    public SecretCryptoService(@Value("${security.secret.master-key:}") String configuredKey) {
        this.masterKey = decodeKey(configuredKey);
    }

    public boolean isReady() { return masterKey != null; }

    public EncryptedValue encrypt(String secretName, String plainText) {
        requireReady();
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(secretName.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return new EncryptedValue(Base64.getEncoder().encodeToString(encrypted), Base64.getEncoder().encodeToString(iv));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Secret encryption failed", ex);
        }
    }

    public String decrypt(String secretName, String cipherText, String initializationVector) {
        requireReady();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = Base64.getDecoder().decode(initializationVector);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(secretName.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Stored secret cannot be decrypted", ex);
        }
    }

    private void requireReady() {
        if (!isReady()) throw new IllegalStateException("HNBLUE secret encryption key is not configured");
    }

    private byte[] decodeKey(String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(configuredKey.trim());
            if (decoded.length != 32) throw new IllegalStateException("HNBLUE_SECRET_ENCRYPTION_KEY must decode to exactly 32 bytes");
            return decoded;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("HNBLUE_SECRET_ENCRYPTION_KEY must be Base64 encoded", ex);
        }
    }

    public record EncryptedValue(String cipherText, String initializationVector) {}
}
