/**
 * 用户认证与权限管理控制器
 *
 * 功能概述：
 * • 提供用户登录、注册、密码重置等核心认证功能
 * • 支持管理员和普通用户的双重身份认证体系
 * • 实现基于令牌(JWT)的无状态认证机制
 * • 集成Redis实现分布式会话管理和限流控制
 *
 * 安全特性：
 * • 请求频率限制防止暴力破解
 * • 密码重置双因素认证流程
 * • 输入参数格式严格校验
 * • 令牌验证与状态管理
 *
 * 业务流程：
 * • 登录流程 - 区分管理员/用户身份并颁发令牌
 * • 注册流程 - 用户信息校验与账户创建
 * • 密码重置 - 邮件验证与安全令牌机制
 * • 状态查询 - 实时操作进度跟踪
 *
 * 技术集成：
 * • JWT令牌生成与验证
 * • Redis缓存状态管理
 * • 邮件服务集成
 * • 请求限流保护
 */
package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.repository.UserRepository;
import com.example.jpaspringboot.service.AccessLimit;
import com.example.jpaspringboot.service.impl.AdminServiceImpl;
import com.example.jpaspringboot.service.impl.UserServiceImpl;
import com.example.jpaspringboot.service.impl.UserActivityLogServiceImpl;
import com.example.jpaspringboot.util.JwtUtils;
import com.example.jpaspringboot.util.RedisUtil;
import com.example.jpaspringboot.util.PasswordPolicy;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserServiceImpl userServiceImpl;      // 用户业务服务

    @Autowired
    private AdminServiceImpl adminServiceImpl;    // 管理员业务服务

    @Autowired
    private RedisUtil redisUtil;                  // Redis缓存工具

    @Autowired
    private UserActivityLogServiceImpl activityLogService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 用户登录接口
     * 支持管理员和普通用户双重身份认证
     *
     * @param loginRequest 登录请求体（用户名、密码）
     * @return 认证结果及JWT令牌
     */
    @PostMapping("/login")
    @AccessLimit(seconds = 3, maxCount = 10)  // 登录接口限流，Redis 不可用时由拦截器降级放行
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest == null
                || loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()
                || loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "请填写用户名和密码"));
        }

        String username = loginRequest.getUsername().trim();
        String password = loginRequest.getPassword();

        try {
            if (adminServiceImpl.authenticateAdmin(username, password)) {
                String token = JwtUtils.generateToken(username);
                return ResponseEntity.ok().body(new LoginResponse("Admin", token));
            }

            if (userServiceImpl.authenticateUser(username, password)) {
                String token = JwtUtils.generateToken(username);
                userServiceImpl.markLastLogin(username);
                activityLogService.record(null, username, "User", userServiceImpl.getUserIDByToken(token), "LOGIN_SUCCESS", "登录成功", "/api/login", "SUCCESS", null);
                return ResponseEntity.ok().body(new LoginResponse("User", token));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        } catch (Exception ex) {
            logger.error("/api/login failed for user: {}", username, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "登录接口异常，请检查 hnblue_v2_dev_control 中 user/admin 表字段、Redis 限流服务和后端日志。",
                    "error", ex.getClass().getSimpleName()
            ));
        }
    }

    /**
     * 用户注册接口
     * 执行完整的用户信息校验和账户创建流程
     *
     * @param registerRequest 注册请求体
     * @return 注册结果响应
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        if (registerRequest == null) {
            return badRequest("请填写注册信息");
        }

        String username = normalizeUsername(registerRequest.getUsername());
        String email = normalizeEmail(registerRequest.getEmail());
        String birthdate = normalizeBirthdate(registerRequest.getBirthdate());
        String password = registerRequest.getPassword();

        if (username == null || !username.matches("^[a-zA-Z0-9_-]{3,20}$")) {
            return badRequest("用户名需为 3-20 位字母、数字、下划线或短横线");
        }
        if (!PasswordPolicy.isValid(password)) {
            return badRequest(PasswordPolicy.MESSAGE);
        }
        if (email == null || !email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
            return badRequest("邮箱格式不正确");
        }
        if (birthdate == null) {
            return badRequest("生日格式应为 YYYY-MM-DD");
        }

        if (userRepository.existsByName(username)) {
            return badRequest("用户名已存在");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return badRequest("邮箱已存在");
        }

        try {
            boolean isRegistered = userServiceImpl.registerUser(username, password, email, birthdate);
            return isRegistered ? ResponseEntity.ok().body("Registration successful!")
                    : ResponseEntity.badRequest().body("注册失败，请检查用户名或邮箱是否已存在");
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            logger.warn("/api/register unique constraint conflict for username={} emailHash={}", username, Integer.toHexString(email.hashCode()));
            return badRequest("用户名或邮箱已存在，请更换后重试");
        }
    }

    @PostMapping("/initiateReset")
    public ResponseEntity<?> initiateReset(@RequestBody ResetRequest resetRequest) {
        if (resetRequest == null || !PasswordPolicy.isValid(resetRequest.getNewPassword())) {
            return badRequest(PasswordPolicy.MESSAGE);
        }
        String username = normalizeUsername(resetRequest.getUsername());
        String email = normalizeEmail(resetRequest.getEmail());

        boolean isUserVerified = userServiceImpl.verifyUser(username, email, resetRequest.getNewPassword());

        if (!isUserVerified) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User verification failed. No matching records found or no emails sent");
        }

        redisUtil.set("resetPassword:" + username, "等待用户点击中", 600);
        return ResponseEntity.ok().body("验证邮件已发送。请检查您的邮箱完成密码重置过程。");
    }

    @GetMapping("/verifyToken")
    public ResponseEntity<?> verifyTokenAndResetPassword(@RequestParam("token") String token,
                                                         @RequestParam("newPassword") String encryptedPassword) {
        // 从令牌中提取用户ID
        Integer userId = JwtUtils.extractUserId(token);
        String username = userServiceImpl.getUsernameById(userId);

        // 更新重置状态为身份验证中
        redisUtil.set("resetPassword:" + username, "等待用户身份验证中", 120);

        // 验证令牌有效性
        boolean isVerified = userServiceImpl.verifyToken(token);
        if (!isVerified) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }

        // 检查重置状态是否有效
        String status = (String) redisUtil.get("resetPassword:" + username);
        if (!"等待用户身份验证中".equals(status)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Request expired.");
        }

        // 解密并重置密码
        String decryptedPassword = decryptNewPassword(encryptedPassword);
        boolean isPasswordReset = userServiceImpl.resetUserPassword(username, decryptedPassword);

        if (isPasswordReset) {
            // 更新重置状态为已完成
            redisUtil.set("resetPassword:" + username, "已成功修改密码", 1200);
            return ResponseEntity.ok().body("Password reset successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Password reset failed.");
        }
    }

    /**
     * 密码重置状态查询接口
     * 提供前端轮询查询密码重置操作进度
     *
     * @param username 用户名
     * @return 当前重置操作状态
     */
    @GetMapping("/checkResetStatus")
    public ResponseEntity<?> checkResetStatus(@RequestParam String username) {
        String status = (String) redisUtil.get("resetPassword:" + username);

        if ("已成功修改密码".equals(status)) {
            return ResponseEntity.ok().body(Map.of("status", "completed",
                    "message", "Password reset successfully completed."));
        } else if ("等待用户点击中".equals(status) || "等待用户身份验证中".equals(status)) {
            return ResponseEntity.ok().body(Map.of("status", "in progress",
                    "message", "Reset in progress..."));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "failed",
                    "message", "Password reset failed or expired."));
        }
    }

    /**
     * Base64密码解密方法
     *
     * @param encryptedPassword 加密的密码字符串
     * @return 解密后的明文密码
     */
    private String decryptNewPassword(String encryptedPassword) {
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedPassword);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String value = username.trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String value = email.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }

    private String normalizeBirthdate(String birthdate) {
        if (birthdate == null) {
            return null;
        }
        String value = birthdate.trim().replace('/', '-');
        try {
            return LocalDate.parse(value).toString();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    // ============================ 内部请求体类定义 ============================

    /**
     * 登录请求数据载体
     */
    public static class LoginRequest {
        private String username;
        private String password;

        // Getter和Setter方法
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        @Override
        public String toString() {
            return "LoginRequest{username=" + username + ", password=[PROTECTED]}";
        }
    }

    /**
     * 注册请求数据载体
     */
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String birthdate;

        // Getter和Setter方法
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getBirthdate() { return birthdate; }
        public void setBirthdate(String birthdate) { this.birthdate = birthdate; }

        @Override
        public String toString() {
            return "RegisterRequest{username=" + username + ", password=[PROTECTED]" +
                    ", email=" + email + ", birthdate=" + birthdate + "}";
        }
    }

    /**
     * 密码重置请求数据载体
     */
    public static class ResetRequest {
        private String username;
        private String email;
        private String newPassword;

        // Getter和Setter方法
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    /**
     * 登录响应数据载体
     */
    public static class LoginResponse {
        private String userType;  // 用户类型：Admin/User
        private String token;     // JWT认证令牌

        public LoginResponse(String userType, String token) {
            this.userType = userType;
            this.token = token;
        }

        // Getter和Setter方法
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        @Override
        public String toString() {
            return "LoginResponse{userType=" + userType + ", token=[PROTECTED]}";
        }
    }
}
