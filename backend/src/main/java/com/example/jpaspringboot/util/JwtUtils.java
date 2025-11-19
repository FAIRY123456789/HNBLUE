package com.example.jpaspringboot.util;

import com.example.jpaspringboot.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;


import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {
    private static final long EXPIRE_DURATION = 604800; // 十分钟，以毫秒为单位
    // 秘钥，确保至少为 256 位
    private static String secret = "abcdfghiabcdfghiabcdfghiabcdfghi";

    // 从字符串密钥生成密钥对象
    private static Key key = Keys.hmacShaKeyFor(secret.getBytes());

    /**
     *
     在JWT中添加额外的声明（如管理员ID）不会影响Token的基本功能。JWT（JSON Web Token）是一种用于安全传输信息的紧凑且自包含的方式。
     在JWT中添加额外的声明是一种常见做法，用于存储有关用户的特定信息，这可以在验证Token时被应用程序用来识别用户或执行其他操作。
     * @param username
     * @return
     */
    // 生成token
    public static String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 1000 *EXPIRE_DURATION);
        //注意：Autowired不能用于填充方法内的本地变量，必须要在整个类中定义！这个问题出现过！
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(username)
                .claim("userName",username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();

        return claims.getSubject();
    }

    // 生成带有额外声明的token
//    public static String generateTokenWithClaims(String username, Map<String, Object> claims) {
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + EXPIRE_DURATION);
//
//        JwtBuilder builder = Jwts.builder()
//                .setSubject(username)
//                .setIssuedAt(now)
//                .setExpiration(expiryDate)
//                .signWith(key, SignatureAlgorithm.HS256);
//
//        claims.forEach(builder::claim);
//
//        return builder.compact();
//    }

    /**
     * 生成带有额外声明的token，包括用户ID、用户名和邮箱
     *
     * @param user 用户对象
     * @return 生成的JWT字符串
     */
    public static String generateTokenWithClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getName());
        claims.put("email", user.getEmail());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRE_DURATION);

        JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getName())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256);

        return builder.compact();
    }

//    除了extractUserId，我创新了两组生成与验证的方法。不仅确保了用户ID的匹配，还增加了用户名和邮箱的检查，大大提高了安全性。
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

    // 从token中提取特定声明
    public static Object getClaimFromToken(String token, String claimKey) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get(claimKey);
    }
    // 验证token是否过期
    public static boolean isTokenExpired(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration().before(new Date());
    }
    // 从token中提取用户ID
    public static Integer extractUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Integer.class);  // 确保在生成token时已经设置了userId
    }

    public static String extractUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userName", String.class);  // 确保在生成token时已经设置了userId
    }
}
