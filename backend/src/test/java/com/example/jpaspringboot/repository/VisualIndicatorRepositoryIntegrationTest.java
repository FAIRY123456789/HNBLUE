package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.VisualIndicatorData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VisualIndicatorRepositoryIntegrationTest
 *
 * 本测试类对 VisualIndicatorRepository 进行集成测试，主要验证以下场景：
 * 1. 根据年份查询指标数据（testFindByYear_returnsCorrectData）
 * 2. 根据区域名、指标键和年份查询唯一数据（testFindByRegionIndicatorYear_returnsOne）
 *
 * 使用 @DataJpaTest 进行 JPA 层测试，专注于数据库操作的正确性校验。
 */
@DataJpaTest
public class VisualIndicatorRepositoryIntegrationTest {

    @Autowired
    private VisualIndicatorRepository repository;

    /**
     * 测试方法：testFindByYear_returnsCorrectData
     *
     * 测试场景：验证根据年份查询指标数据是否正确。
     * 步骤：
     * 1. 构建并保存一个 VisualIndicatorData 实体。
     * 2. 通过年份 2024 查询数据列表。
     * 3. 验证返回列表非空，且首个数据的区域名称和指标键正确。
     */
    @Test
    public void testFindByYear_returnsCorrectData() {
        // 构建测试数据
        VisualIndicatorData data = new VisualIndicatorData(
                null, // id 设为 null 由数据库自动生成
                "儋州",
                "SOC",
                42.5,
                "t/ha",
                "土壤有机碳储量",
                2024
        );
        repository.save(data);

        // 查询数据
        List<VisualIndicatorData> results = repository.findByYear(2024);

        // 断言校验
        assertFalse(results.isEmpty());
        assertEquals("儋州", results.get(0).getRegionName());
        assertEquals("SOC", results.get(0).getIndicatorKey());
    }

    /**
     * 测试方法：testFindByRegionIndicatorYear_returnsOne
     *
     * 测试场景：验证根据区域名、指标键和年份查询唯一数据是否正确。
     * 步骤：
     * 1. 构建并保存一个 VisualIndicatorData 实体。
     * 2. 通过区域名称、指标键和年份查询数据。
     * 3. 验证返回对象非空，且值和单位正确。
     */
    @Test
    public void testFindByRegionIndicatorYear_returnsOne() {
        // 构建测试数据
        VisualIndicatorData data = new VisualIndicatorData(
                null, // id 设为 null 由数据库自动生成
                "文昌",
                "CH4",
                12.3,
                "mg/m²/h",
                "甲烷通量",
                2024
        );
        repository.save(data);

        // 查询数据
        VisualIndicatorData found = repository.findByRegionNameAndIndicatorKeyAndYear("文昌", "CH4", 2024);

        // 断言校验
        assertNotNull(found);
        assertEquals(12.3, found.getValue());
        assertEquals("mg/m²/h", found.getUnit());
    }
}
