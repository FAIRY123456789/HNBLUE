/**
 * AES加密解密工具类
 *
 * 功能概述：
 * • 基于AES-CTR模式提供对称加密解密功能
 * • 集成HMAC-SHA256实现数据完整性验证
 * • 使用PBKDF2密钥派生算法增强密码安全性
 *
 * 安全特性：
 * • CTR模式加密避免填充预言攻击
 * • 随机盐值防止彩虹表攻击
 * • HMAC验证确保数据完整性
 * • 10万次PBKDF2迭代抵抗暴力破解
 *
 * 加密流程：
 * 1. 生成随机盐值和初始化向量
 * 2. 使用PBKDF2派生AES-256密钥
 * 3. AES-CTR模式加密明文数据
 * 4. HMAC-SHA256计算消息认证码
 * 5. 组合所有组件为Base64编码字符串
 *
 * 使用场景：
 * • 敏感配置信息加密存储
 * • 用户隐私数据保护
 * • 安全通信数据加密
 * • 数据库字段级加密
 *
 * 注意事项：
 * • 密码强度直接影响加密安全性
 * • 妥善保管加密密钥和密码
 * • 定期更换加密密钥增强安全性
 */
package com.example.jpaspringboot.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class MyEncryptor {

    // 加密算法配置
    private static final String ALGORITHM = "AES";
    private static final String MODE = "AES/CTR/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    // 安全参数配置
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 100000;

    /**
     * 加密明文数据
     *
     * @param plainText 待加密的明文数据
     * @param password 加密密码
     * @return Base64编码的加密数据字符串
     * @throws Exception 加密过程中出现的异常
     */
    public static String encrypt(String plainText, String password) throws Exception {
        // 生成随机盐值和初始化向量
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        SecretKey secretKey = deriveKey(password, salt);

        // 初始化加密向量
        byte[] iv = new byte[SALT_LENGTH];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // 执行AES-CTR加密
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 生成HMAC消息认证码
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKey);
        mac.update(encryptedBytes);
        byte[] hmac = mac.doFinal();

        // 组合所有组件：盐值 + 初始化向量 + HMAC + 加密数据
        byte[] combined = new byte[salt.length + iv.length + hmac.length + encryptedBytes.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(hmac, 0, combined, salt.length + iv.length, hmac.length);
        System.arraycopy(encryptedBytes, 0, combined, salt.length + iv.length + hmac.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 解密密文数据
     *
     * @param encryptedText Base64编码的加密数据字符串
     * @param password 解密密码
     * @return 解密后的明文数据
     * @throws Exception 解密过程中出现的异常
     */
    public static String decrypt(String encryptedText, String password) throws Exception {

        // 解码Base64并分离各个组件
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        // 提取盐值、初始化向量、HMAC和加密数据
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[SALT_LENGTH];
        byte[] hmac = new byte[32]; // HMAC-SHA256固定32字节
        byte[] encryptedBytes = new byte[combined.length - SALT_LENGTH - SALT_LENGTH - hmac.length];

        System.arraycopy(combined, 0, salt, 0, salt.length);
        System.arraycopy(combined, salt.length, iv, 0, iv.length);
        System.arraycopy(combined, salt.length + iv.length, hmac, 0, hmac.length);
        System.arraycopy(combined, salt.length + iv.length + hmac.length, encryptedBytes, 0, encryptedBytes.length);

        // 派生解密密钥
        SecretKey secretKey = deriveKey(password, salt);

        // 验证HMAC完整性
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKey);
        mac.update(encryptedBytes);
        byte[] computedHmac = mac.doFinal();

        if (!java.util.Arrays.equals(hmac, computedHmac)) {
            throw new IllegalArgumentException("Invalid HMAC, data might be tampered");
        }

        // 执行AES-CTR解密
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 使用PBKDF2算法从密码和盐值派生AES密钥
     *
     * @param password 用户密码
     * @param salt 随机盐值
     * @return 派生的AES-256密钥
     * @throws NoSuchAlgorithmException 算法不支持异常
     * @throws InvalidKeySpecException 密钥规范无效异常
     */
    private static SecretKey deriveKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);  // AES-256
        SecretKey tmp = javax.crypto.SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
}
