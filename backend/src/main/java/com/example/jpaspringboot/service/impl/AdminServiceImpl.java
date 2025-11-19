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

    @Override
    public void generateAndSaveTestAdmins() {
        for (int i = 100000; i <= 100002; i++) {
            String username = "Admin_" + i;
            // 这是一个测试密码，实际应用中请勿使用过于简单的密码。
            String password = "123456";
            addAdmin(username, password);
        }
    }

    @Override
    public @ResponseBody int addAdmin(@RequestParam String name, @RequestParam String password) {
        Admin existingAdmin = adminRepository.findByName(name);
        if (existingAdmin == null) {
            try {
                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);
                Admin newAdmin = new Admin(name, salt, passwordHash);
                newAdmin = adminRepository.save(newAdmin);
                System.out.println("用户添加成功!");

                return newAdmin.getId(); // 仅返回新管理员的ID
            } catch (Exception e) {
                System.out.println("添加用户出错: " + e.getMessage());
                throw new RuntimeException("Fail to add user!"); // 选择抛出一个更具体的异常
            }
        } else {
            throw new UserAlreadyExistsException("Fail to add user! Username already exists!"); // 抛出自定义异常
        }
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
        //使用Base64编码盐是因为我们在数据库中存储的是字符串。可以选择存储原始字节或使用其他编码方法，但Base64是常用且方便的选择。
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 添加盐到密码前并计算哈希
            String saltedPassword = salt + password;
            byte[] hashedBytes = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }


    public boolean authenticateAdmin(String name, String inputPassword) {
        Admin admin = adminRepository.findByName(name);
        if (admin == null) {
            return false;
        }

        String expectedHash = hashPassword(inputPassword, admin.getSalt());
        return expectedHash.equals(admin.getPasswordHash());
    }
}
