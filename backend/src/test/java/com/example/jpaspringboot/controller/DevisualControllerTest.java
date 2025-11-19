/**
 * DevisualController 集成测试类
 *
 * 功能概述：
 * • 对数据可视化控制器进行完整的集成测试
 * • 验证区域数据查询接口的完整性和正确性
 * • 确保API响应结构和数据格式符合预期规范
 *
 * 测试策略：
 * • 使用Spring Boot Test进行全栈集成测试
 * • 通过MockMvc模拟HTTP请求和验证响应
 * • 采用随机端口避免测试环境冲突
 *
 * 测试场景覆盖：
 * • 区域数据查询接口功能验证
 * • JSON响应结构完整性检查
 * • 数据字段类型和格式验证
 *
 * 技术特性：
 * • 自动配置MockMvc测试环境
 * • 集成Hamcrest断言库进行响应验证
 * • 支持真实数据库连接测试
 */
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

@SpringBootTest(classes = JpAspringbootApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DevisualControllerTest {

    // MockMvc用于模拟HTTP请求和验证响应
    @Autowired
    private MockMvc mockMvc;

    // 随机端口号，避免测试环境冲突
    @LocalServerPort
    private int port;

    /**
     * 区域数据查询接口集成测试
     *
     * 测试目标：验证通过区域名称获取完整数据集的接口功能
     * 测试数据：使用"琼海"作为测试区域（需确保测试数据库中存在对应数据）
     * 验证要点：
     * - 接口响应状态和内容类型
     * - 返回数据结构的完整性
     * - 各数据字段的类型和格式正确性
     *
     * @throws Exception 测试过程中可能出现的异常
     */
    @Test
    public void testGetRegionAllData_thenSuccess() throws Exception {
        // 测试数据准备 - 使用固定区域名称进行查询
        String testRegion = "琼海";

        // 执行GET请求并验证响应
        mockMvc.perform(get("/api/devisual/{regionName}", testRegion))
                .andExpect(status().isOk())                    // 验证HTTP 200状态码
                .andExpect(content().contentType("application/json")) // 验证响应内容类型为JSON
                .andExpect(jsonPath("$.regionInfo").exists())         // 验证基础区域信息字段存在
                .andExpect(jsonPath("$.carbonTrends").isArray())      // 验证碳储趋势数据为数组格式
                .andExpect(jsonPath("$.fluxComposition").isArray())   // 验证通量组成为数组格式
                .andExpect(jsonPath("$.multiCarbonMetrics").exists()) // 验证多碳指标字段存在
                .andExpect(jsonPath("$.yearlyTrends").isArray())      // 验证年度趋势数据为数组格式
                .andExpect(jsonPath("$.fluxSamples").isArray())       // 验证通量样本数据为数组格式
                .andExpect(jsonPath("$.speciesPie").isArray())        // 验证物种分布数据为数组格式
                .andExpect(jsonPath("$.economyProjections").isArray()); // 验证经济预测数据为数组格式
    }
}