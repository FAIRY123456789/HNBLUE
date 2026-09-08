/**
 * JWT令牌生成与验证工具类
 *
 * 功能概述：
 * • 基于JJWT库实现JWT令牌的生成、解析和验证
 * • 支持标准声明和自定义声明的令牌生成
 * • 提供完整的令牌生命周期管理功能
 *
 * 核心特性：
 * • 支持HS256签名算法确保令牌完整性
 * • 包含令牌过期时间自动管理
 * • 支持多维度用户信息验证（用户名、邮箱、用户ID）
 * • 提供声明数据的灵活提取接口
 *
 * 安全机制：
 * • 使用256位密钥进行HMAC-SHA256签名
 * • 自动验证令牌有效期防止过期使用
 * • 多因素身份验证增强安全性
 *
 * 使用场景：
 * • 用户登录认证令牌生成
 * • API接口访问权限验证
 * • 用户会话状态管理
 * • 微服务间安全通信
 */
package com.example.jpaspringboot.util;

import com.example.jpaspringboot.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {

    private static final long DEFAULT_TTL_SECONDS = 604800L;
    private static final long EXPIRE_DURATION_MILLIS = resolveTtlMillis();
    private static final Key key = resolveSigningKey();

    private static Key resolveSigningKey() {
        String configured = firstNonBlank(
                System.getenv("HNBLUE_JWT_SECRET"),
                System.getProperty("hnblue.jwt.secret")
        );
        if (configured == null) {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            System.err.println("HNBLUE_JWT_SECRET is not set; using a process-local signing key. Existing tokens will be invalid after restart.");
            return Keys.hmacShaKeyFor(generated);
        }
        byte[] bytes = configured.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("HNBLUE_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private static long resolveTtlMillis() {
        String configured = firstNonBlank(
                System.getenv("HNBLUE_JWT_TTL_SECONDS"),
                System.getProperty("hnblue.jwt.ttl-seconds")
        );
        try {
            long seconds = configured == null ? DEFAULT_TTL_SECONDS : Long.parseLong(configured);
            if (seconds <= 0) throw new NumberFormatException("non-positive value");
            return Math.multiplyExact(seconds, 1000L);
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalStateException("HNBLUE_JWT_TTL_SECONDS must be a positive integer", ex);
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first.trim();
        if (second != null && !second.isBlank()) return second.trim();
        return null;
    }

    /**
     * 生成基础JWT令牌（仅包含用户名）
     *
     * @param username 用户名主体
     * @return 生成的JWT令牌字符串
     */
    public static String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRE_DURATION_MILLIS);

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(username)
                .claim("userName",username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从JWT令牌中提取用户名
     *
     * @param token JWT令牌字符串
     * @return 用户名主体
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();

        return claims.getSubject();
    }

    /**
     * 生成包含用户详细信息的JWT令牌
     * 自定义声明包含用户ID、用户名和邮箱信息
     *
     * @param user 用户实体对象
     * @return 生成的JWT令牌字符串
     */
    public static String generateTokenWithClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getName());
        claims.put("email", user.getEmail());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRE_DURATION_MILLIS);

        JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getName())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256);

        return builder.compact();
    }

    /**
     * 验证令牌与用户信息的匹配性
     * 验证维度：用户名、邮箱匹配性及令牌有效期
     *
     * @param token JWT令牌字符串
     * @param user 用户实体对象
     * @return 验证结果（true-验证通过，false-验证失败）
     */
    public static boolean validateToken(String token, User user) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        boolean usernameMatch = claims.get("username").equals(user.getName());
        boolean emailMatch = claims.get("email").equals(user.getEmail());
        boolean notExpired = claims.getExpiration().after(new Date());

        return usernameMatch && emailMatch && notExpired;
    }

    /**
     * 从令牌中提取指定声明的值
     *
     * @param token JWT令牌字符串
     * @param claimKey 声明键名
     * @return 声明对应的值对象
     */
    public static Object getClaimFromToken(String token, String claimKey) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get(claimKey);
    }

    /**
     * 验证令牌是否已过期
     *
     * @param token JWT令牌字符串
     * @return 过期状态（true-已过期，false-未过期）
     */
    public static boolean isTokenExpired(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration().before(new Date());
    }

    /**
     * 从令牌中提取用户ID
     *
     * @param token JWT令牌字符串
     * @return 用户ID整数值
     */
    public static Integer extractUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Integer.class);  // 确保在生成token时已经设置了userId
    }

    /**
     * 从令牌中提取用户名
     *
     * @param token JWT令牌字符串
     * @return 用户名字符串
     */
    public static String extractUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userName", String.class);  // 确保在生成token时已经设置了userName
    }
}
