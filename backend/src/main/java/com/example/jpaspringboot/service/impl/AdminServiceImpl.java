/**
 * 管理员服务实现类
 *
 * 功能概述：
 * • 管理员账户的创建、认证和安全管理
 * • 基于SHA-256加盐哈希的密码安全存储
 * • 批量测试管理员账户生成
 *
 * 安全特性：
 * • 使用SecureRandom生成密码盐值
 * • SHA-256哈希算法保护密码安全
 * • Base64编码存储盐值和哈希结果
 * • 用户名唯一性校验防止重复注册
 *
 * 核心业务：
 * • 管理员账户注册与密码加密
 * • 登录认证与密码验证
 * • 测试数据初始化
 *
 * 异常处理：
 * • UserAlreadyExistsException - 用户名重复异常
 * • RuntimeException - 系统级异常封装
 */
package com.example.jpaspringboot.service.impl;

import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.exception.UserAlreadyExistsException;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    AdminRepository adminRepository;

    /**
     * 生成测试管理员账户
     * 创建三个测试管理员：Admin_100000, Admin_100001, Admin_100002
     * 注意：实际生产环境应使用强密码并禁用测试账户
     */
    @Override
    public void generateAndSaveTestAdmins() {
        for (int i = 100000; i <= 100002; i++) {
            String username = "Admin_" + i;
            // 测试密码，生产环境应使用复杂密码
            String password = "123456";
            addAdmin(username, password);
        }
    }

    /**
     * 添加新管理员账户
     * @param name 管理员用户名
     * @param password 明文密码
     * @return 新管理员ID
     * @throws UserAlreadyExistsException 用户名已存在时抛出
     * @throws RuntimeException 系统异常时抛出
     */
    @Override
    public @ResponseBody int addAdmin(@RequestParam String name, @RequestParam String password) {
        // 检查用户名是否已存在
        Admin existingAdmin = adminRepository.findByName(name);
        if (existingAdmin == null) {
            try {
                // 生成盐值并计算密码哈希
                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);

                // 创建并保存管理员实体
                Admin newAdmin = new Admin(name, salt, passwordHash);
                newAdmin = adminRepository.save(newAdmin);
                System.out.println("用户添加成功!");

                return newAdmin.getId(); // 返回新管理员的ID
            } catch (Exception e) {
                System.out.println("添加用户出错: " + e.getMessage());
                throw new RuntimeException("用户添加失败!");
            }
        } else {
            throw new UserAlreadyExistsException("用户添加失败! 用户名已存在!");
        }
    }

    /**
     * 生成随机盐值
     * @return Base64编码的16字节随机盐值
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        // 使用Base64编码便于数据库字符串存储
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 计算加盐密码哈希值
     * @param password 明文密码
     * @param salt Base64编码的盐值
     * @return Base64编码的SHA-256哈希值
     */
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 盐值前缀拼接密码
            String saltedPassword = salt + password;
            byte[] hashedBytes = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码哈希计算失败", e);
        }
    }

    /**
     * 管理员登录认证
     * @param name 管理员用户名
     * @param inputPassword 输入的明文密码
     * @return 认证结果 true-成功 false-失败
     */
    public boolean authenticateAdmin(String name, String inputPassword) {
        Admin admin = adminRepository.findByName(name);
        if (admin == null) {
            return false;
        }

        // 使用存储的盐值计算输入密码的哈希值
        String expectedHash = hashPassword(inputPassword, admin.getSalt());
        return expectedHash.equals(admin.getPasswordHash());
    }
}