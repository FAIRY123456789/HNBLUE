package com.example.jpaspringboot.converter;

import com.example.jpaspringboot.util.MyEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Optional JPA converter for encrypted string fields.
 *
 * <p>The encryption password is deliberately not stored in source control.
 * Set {@code HNBLUE_FIELD_ENCRYPTION_SECRET} before using an entity field that
 * references this converter.</p>
 */
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    private static String encryptionSecret() {
        String configured = System.getenv("HNBLUE_FIELD_ENCRYPTION_SECRET");
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("hnblue.field-encryption-secret");
        }
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException(
                    "HNBLUE_FIELD_ENCRYPTION_SECRET is required when encrypted entity fields are used"
            );
        }
        return configured;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            return MyEncryptor.encrypt(attribute, encryptionSecret());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt entity field", ex);
        }
    }

    @Override
    public String convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null) return null;
        try {
            return MyEncryptor.decrypt(databaseValue, encryptionSecret());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt entity field", ex);
        }
    }
}
