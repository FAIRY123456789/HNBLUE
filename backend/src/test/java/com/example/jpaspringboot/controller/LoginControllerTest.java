/**
 * 用户登录控制器集成测试类
 *
 * 功能概述：
 * • 验证用户注册和登录功能的完整业务流程
 * • 测试正常场景和异常场景的接口响应
 * • 确保认证系统的可靠性和安全性
 *
 * 测试场景覆盖：
 * • 用户注册成功后登录验证
 * • 错误密码登录失败验证
 * • HTTP状态码和响应内容校验
 *
 * 技术特性：
 * • 使用Spring Boot Test进行集成测试
 * • 结合MockMvc模拟HTTP请求和响应
 * • 动态生成测试数据避免冲突
 * • JSON路径断言验证响应结构
 *
 * 测试策略：
 * • 端到端测试认证业务流程
 * • 验证令牌生成和用户类型返回
 * • 检查错误处理和安全防护机制
 */
package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.JpAspringbootApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = JpAspringbootApplication.class)
@AutoConfigureMockMvc
public class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;  // 模拟MVC测试框架，用于发送HTTP请求

    private final ObjectMapper objectMapper = new ObjectMapper();  // JSON序列化工具

    /**
     * 测试用户注册成功后登录的完整业务流程
     * 验证用户生命周期：注册 → 登录 → 令牌获取
     *
     * @throws Exception 测试过程中可能出现的异常
     */
    @Test
    public void testRegister_thenLogin_Success() throws Exception {
        // 生成唯一用户名避免测试数据冲突
        String randomUsername = "tu_" + System.currentTimeMillis();

        // 构建用户注册请求体
        var registerBody = Map.of(
                "username", randomUsername,
                "password", "test12345",
                "email", randomUsername + "@example.com",
                "birthdate", "2000-01-01"
        );

        // 执行用户注册请求并验证响应
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isOk())  // 验证HTTP 200状态码
                .andExpect(content().string(org.hamcrest.Matchers.containsString("successful")));  // 验证成功消息

        // 构建登录请求体
        var loginBody = Map.of(
                "username", randomUsername,
                "password", "test12345"
        );

        // 执行用户登录请求并验证认证响应
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())  // 验证HTTP 200状态码
                .andExpect(jsonPath("$.token").exists())  // 验证JWT令牌存在
                .andExpect(jsonPath("$.userType").value("User"));  // 验证用户类型正确
    }

    /**
     * 测试错误密码登录失败场景
     * 验证系统对无效凭据的安全处理机制
     *
     * @throws Exception 测试过程中可能出现的异常
     */
    @Test
    public void testLogin_FailedWithWrongPassword() throws Exception {
        // 构建错误凭据登录请求体
        var loginBody = Map.of(
                "username", "zhangsansan",
                "password", "wrongpass"  // 故意使用错误密码
        );

        // 执行登录请求并验证认证失败响应
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized())  // 验证HTTP 401未授权状态码
                .andExpect(jsonPath("$.message").exists());  // 验证失败消息
    }

    @Test
    public void testPerformanceAccount_IsBlockedByDefault() throws Exception {
        String username = "perf_user_" + String.valueOf(System.currentTimeMillis()).substring(7);
        var registerBody = Map.of(
                "username", username,
                "password", "test12345",
                "email", username + "@example.com",
                "birthdate", "2000-01-01"
        );

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "test12345"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }
}
