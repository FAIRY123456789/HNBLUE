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

/*MyEncryptor 推荐放在 util 包内。这是因为它是一个工具类，用于执行特定的操作（加密和解密）*/
public class MyEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String MODE = "AES/CTR/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 100000;

    public static String encrypt(String plainText, String password) throws Exception {
        // 生成salt和密钥
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        SecretKey secretKey = deriveKey(password, salt);

        // 加密
        byte[] iv = new byte[SALT_LENGTH];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 生成HMAC
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKey);
        mac.update(encryptedBytes);
        byte[] hmac = mac.doFinal();

        // 合并 salt, iv, hmac 和 加密文本 以便存储和后续解密
        byte[] combined = new byte[salt.length + iv.length + hmac.length + encryptedBytes.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(hmac, 0, combined, salt.length + iv.length, hmac.length);
        System.arraycopy(encryptedBytes, 0, combined, salt.length + iv.length + hmac.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String encryptedText, String password) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        // 从组合的数据中提取 salt, iv, hmac 和 加密的文本
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[SALT_LENGTH];
        byte[] hmac = new byte[32];
        byte[] encryptedBytes = new byte[combined.length - SALT_LENGTH - SALT_LENGTH - hmac.length];

        System.arraycopy(combined, 0, salt, 0, salt.length);
        System.arraycopy(combined, salt.length, iv, 0, iv.length);
        System.arraycopy(combined, salt.length + iv.length, hmac, 0, hmac.length);
        System.arraycopy(combined, salt.length + iv.length + hmac.length, encryptedBytes, 0, encryptedBytes.length);

        SecretKey secretKey = deriveKey(password, salt);

        // 验证 HMAC
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKey);
        mac.update(encryptedBytes);
        byte[] computedHmac = mac.doFinal();

        if (!java.util.Arrays.equals(hmac, computedHmac)) {
            throw new IllegalArgumentException("Invalid HMAC, data might be tampered");
        }

        // 解密
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);  // AES-256
        SecretKey tmp = javax.crypto.SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
}
