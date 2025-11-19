package com.example.jpaspringboot.converter;

import com.example.jpaspringboot.util.MyEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/***
 * AttributeConverter 的实现建议放在 converter 包内。
 * 虽然它与JPA实体相关，但为了清晰起见，将其与主要实体类分开是有益的。
 *
 * AttributeConverter对于知道当前上下文或动态获取加密密钥来说可能不是最佳选择。
 * 但是我可以拿它存储写的快乐的事？可以，事情不可能互相公开，允许分享，但平常的个人情况还是加密
 */

@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    private static final String SECRET_PASSWORD = "your-secret-password"; // 最好从配置中获取或使用更安全的管理方法

    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            return MyEncryptor.encrypt(attribute, SECRET_PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            return MyEncryptor.decrypt(dbData, SECRET_PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }
}

