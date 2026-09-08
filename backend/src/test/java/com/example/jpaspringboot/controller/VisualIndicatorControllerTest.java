/**
 * 可视化指标控制器集成测试类
 *
 * 功能概述：
 * • 对VisualIndicatorController进行完整的集成测试验证
 * • 测试REST API接口的响应格式和数据完整性
 * • 验证Spring Boot应用上下文正确加载和配置
 *
 * 测试场景覆盖：
 * • 当前指标数据查询接口的功能验证
 * • JSON响应格式和数据结构校验
 * • HTTP状态码和内容类型验证
 *
 * 技术特性：
 * • 使用随机端口避免测试环境冲突
 * • 集成MockMvc进行Web层模拟测试
 * • 结合Hamcrest断言库进行响应验证
 *
 * 测试策略：
 * • 端到端集成测试验证完整请求链路
 * • 关注接口契约和数据结构稳定性
 * • 确保API返回格式符合前端预期
 */
package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.entity.VisualIndicatorData;
import com.example.jpaspringboot.repository.VisualIndicatorRepository;
import com.example.jpaspringboot.JpAspringbootApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = JpAspringbootApplication.class)
@AutoConfigureMockMvc
public class VisualIndicatorControllerTest {

    // MockMvc用于模拟HTTP请求和验证响应
    @Autowired
    private MockMvc mockMvc;

    // 随机分配的测试服务器端口
    @Autowired
    private VisualIndicatorRepository repository;

    @BeforeEach
    void seedCurrentIndicator() {
        repository.deleteAll();
        repository.save(new VisualIndicatorData(null, "\u743c\u6d77", "mangrove_area", 120.5, "ha", "controller test seed", 2024));
    }

    /**
     * 测试获取当前指标数据接口的成功场景
     *
     * 验证要点：
     * • 接口可访问性及HTTP状态码
     * • 响应内容类型符合JSON格式规范
     * • 返回数据结构完整性校验
     * • 关键业务字段存在性验证
     *
     * @throws Exception 测试过程中可能出现的异常
     */
    @Test
    public void testGetCurrentIndicators_thenSuccess() throws Exception {
        mockMvc.perform(get("/api/indicators/current"))
                .andExpect(status().isOk()) // 验证HTTP 200状态码
                .andExpect(content().contentType("application/json")) // 验证响应内容类型
                .andExpect(jsonPath("$").isArray()) // 验证根节点为数组结构
                .andExpect(jsonPath("$[0].regionName").exists()) // 验证区域名字段存在
                .andExpect(jsonPath("$[0].indicatorKey").exists()) // 验证指标键字段存在
                .andExpect(jsonPath("$[0].value").exists()); // 验证指标值字段存在
    }
}
