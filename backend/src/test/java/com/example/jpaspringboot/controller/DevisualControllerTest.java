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
 * DevisualControllerTest
 *
 * 本测试类对 DevisualController 进行集成测试，主要验证以下场景：
 * 1. 根据区域名称查询所有相关数据（testGetRegionAllData_thenSuccess）
 *
 * 使用 @SpringBootTest 进行集成测试，结合 MockMvc 进行接口调用和响应校验。
 */
@SpringBootTest(classes = JpAspringbootApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DevisualControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @LocalServerPort
    private int port;

    /**
     * 测试方法：testGetRegionAllData_thenSuccess
     *
     * 测试场景：验证通过区域名称查询所有相关数据是否正确。
     * 步骤：
     * 1. 设定区域名称为 "琼海"（确保测试数据库中存在该区域的数据）。
     * 2. 使用 GET 请求访问 /api/devisual/{regionName} 接口。
     * 3. 验证 HTTP 状态码为 200，且返回的内容类型为 JSON。
     * 4. 校验返回的 JSON 结构中包含以下字段：
     *    - regionInfo：基础区域信息，非空字段。
     *    - carbonTrends：碳储与碳通量，数组类型。
     *    - fluxComposition：通量组成，数组类型。
     *    - multiCarbonMetrics：多碳指标数据，非空字段。
     *    - yearlyTrends：多年碳储趋势，数组类型。
     *    - fluxSamples：通量样本，数组类型。
     *    - speciesPie：物种分布饼图，数组类型。
     *    - economyProjections：经济估算柱图，数组类型。
     */
    @Test
    public void testGetRegionAllData_thenSuccess() throws Exception {
        String testRegion = "琼海"; // 请确保数据库中存在该测试区域

        mockMvc.perform(get("/api/devisual/{regionName}", testRegion))
                .andExpect(status().isOk()) // HTTP状态码为200
                .andExpect(content().contentType("application/json")) // 响应内容类型为JSON
                .andExpect(jsonPath("$.regionInfo").exists()) // 检查返回JSON中的regionInfo字段
                .andExpect(jsonPath("$.carbonTrends").isArray()) // 检查carbonTrends是数组
                .andExpect(jsonPath("$.fluxComposition").isArray()) // 检查fluxComposition是数组
                .andExpect(jsonPath("$.multiCarbonMetrics").exists()) // 检查multiCarbonMetrics字段存在
                .andExpect(jsonPath("$.yearlyTrends").isArray()) // 检查yearlyTrends是数组
                .andExpect(jsonPath("$.fluxSamples").isArray()) // 检查fluxSamples是数组
                .andExpect(jsonPath("$.speciesPie").isArray()) // 检查speciesPie是数组
                .andExpect(jsonPath("$.economyProjections").isArray()); // 检查economyProjections是数组
    }
}
