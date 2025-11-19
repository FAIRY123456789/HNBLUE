package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.JpAspringbootApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LoginControllerTest
 *
 * 本测试类对 LoginController 进行集成测试，主要验证以下场景：
 * 1. 用户注册成功并登录验证（testRegister_thenLogin_Success）
 * 2. 使用错误密码登录失败验证（testLogin_FailedWithWrongPassword）
 *
 * 使用 @SpringBootTest 进行集成测试，结合 MockMvc 进行接口调用和响应校验。
 */
@SpringBootTest(classes = JpAspringbootApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试方法：testRegister_thenLogin_Success
     *
     * 测试场景：验证用户注册成功并登录成功。
     * 步骤：
     * 1. 动态生成唯一用户名，构建注册请求体（包含用户名、密码、邮箱、出生日期）。
     * 2. 通过 POST 请求访问 /api/register 进行用户注册。
     * 3. 验证响应状态码为 200，响应中包含 "successful" 字样。
     * 4. 构建登录请求体（用户名和密码）。
     * 5. 通过 POST 请求访问 /api/login 进行用户登录。
     * 6. 验证响应状态码为 200，且返回的 JSON 中包含 token 和 userType 字段。
     */
    @Test
    public void testRegister_thenLogin_Success() throws Exception {
        // 动态生成唯一用户名
        String randomUsername = "testuser_" + System.currentTimeMillis();
        var registerBody = Map.of(
                "username", randomUsername,
                "password", "test123",
                "email", randomUsername + "@example.com",
                "birthdate", "2000-01-01"
        );

        // 用户注册
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isOk()) // HTTP状态码200
                .andExpect(content().string(org.hamcrest.Matchers.containsString("successful")));

        // 构建登录请求体
        var loginBody = Map.of(
                "username", randomUsername,
                "password", "test123"
        );

        // 用户登录
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk()) // HTTP状态码200
                .andExpect(jsonPath("$.token").exists()) // 检查返回JSON中的token字段
                .andExpect(jsonPath("$.userType").value("User")); // 验证userType为User
    }

    /**
     * 测试方法：testLogin_FailedWithWrongPassword
     *
     * 测试场景：验证使用错误密码登录是否失败。
     * 步骤：
     * 1. 构建登录请求体（用户名为 zhangsansan，密码为 wrongpass）。
     * 2. 通过 POST 请求访问 /api/login 进行登录。
     * 3. 验证响应状态码为 401，响应中包含 "Login failed" 字样。
     */
    @Test
    public void testLogin_FailedWithWrongPassword() throws Exception {
        // 构建错误登录请求体
        var loginBody = Map.of(
                "username", "zhangsansan",
                "password", "wrongpass"
        );

        // 错误登录尝试
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized()) // HTTP状态码401
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Login failed")));
    }
}
