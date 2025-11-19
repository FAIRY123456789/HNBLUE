package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.JpAspringbootApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * VisualIndicatorControllerTest
 *
 * 本测试类对 VisualIndicatorController 进行集成测试，主要验证以下场景：
 * 1. 查询当前指标数据并验证响应格式（testGetCurrentIndicators_thenSuccess）
 *
 * 使用 @SpringBootTest 进行集成测试，结合 MockMvc 进行接口调用和响应校验。
 */
@SpringBootTest(classes = JpAspringbootApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class VisualIndicatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @LocalServerPort
    private int port;

    /**
     * 测试方法：testGetCurrentIndicators_thenSuccess
     *
     * 测试场景：验证获取当前指标数据接口返回格式和字段完整性。
     * 步骤：
     * 1. 通过 GET 请求访问 /api/indicators/current 接口。
     * 2. 验证响应状态码为 200（OK）。
     * 3. 验证响应内容类型为 JSON 格式。
     * 4. 校验返回的 JSON 结构为数组类型，且每个元素应包含以下字段：
     *    - regionName：区域名称，非空字段。
     *    - indicatorKey：指标键，非空字段。
     *    - value：指标值，非空字段。
     */
    @Test
    public void testGetCurrentIndicators_thenSuccess() throws Exception {
        mockMvc.perform(get("/api/indicators/current"))
                .andExpect(status().isOk()) // 响应状态码为 200
                .andExpect(content().contentType("application/json")) // 确保返回 JSON
                .andExpect(jsonPath("$").isArray()) // 返回应为数组
                .andExpect(jsonPath("$[0].regionName").exists()) // 第一个对象应含有字段 regionName
                .andExpect(jsonPath("$[0].indicatorKey").exists()) // 含有字段 indicatorKey
                .andExpect(jsonPath("$[0].value").exists()); // 含有字段 value
    }
}
