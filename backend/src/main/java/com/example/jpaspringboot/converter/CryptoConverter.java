/**
 * JPA属性加密转换器（非正式文件，可选择性查看）
 *
 * 功能概述：
 * • 实现JPA AttributeConverter接口，提供实体字段的透明加密解密
 * • 在数据持久化时自动加密，在数据加载时自动解密
 * • 确保数据库存储的敏感信息为密文形式
 *
 * 技术实现：
 * • 基于MyEncryptor工具类进行AES加密解密
 * • 使用固定密钥进行对称加密（生产环境建议改进）
 * • 集成JPA转换器接口实现字段级加密
 *
 * 使用场景：
 * • 用户隐私数据字段加密（手机号、邮箱、身份证号等）
 * • 敏感配置信息加密存储
 * • 需要数据库层面加密的业务字段
 *
 * 安全建议：
 * • 生产环境应从安全配置中心动态获取加密密钥
 * • 建议使用密钥管理系统轮换加密密钥
 * • 对不同的数据分类采用不同的加密策略
 */
package com.example.jpaspringboot.converter;

import com.example.jpaspringboot.util.MyEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CryptoConverter implements AttributeConverter<String, String> {
    // 加密密钥（生产环境建议从配置中心或环境变量获取）
    private static final String SECRET_PASSWORD = "your-secret-password";
    /**
     * 实体字段转换为数据库列（加密过程）
     *
     * @param attribute 实体中的明文属性值
     * @return 加密后的数据库存储值
     * @throws RuntimeException 加密失败时抛出运行时异常
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            return MyEncryptor.encrypt(attribute, SECRET_PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }
    /**
     * 数据库列转换为实体字段（解密过程）
     *
     * @param dbData 数据库中的加密数据
     * @return 解密后的实体属性值
     * @throws RuntimeException 解密失败时抛出运行时异常
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            return MyEncryptor.decrypt(dbData, SECRET_PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }
}

