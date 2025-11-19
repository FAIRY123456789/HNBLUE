/**
 * VisualIndicatorServiceImplTest
 *
 * 本测试类对 VisualIndicatorServiceImpl 进行集成测试，主要验证以下场景：
 * 1. 获取当前所有指标数据（getAllCurrentData_shouldReturnList）
 * 2. 更新指标值成功（updateIndicatorValue_shouldUpdateSuccessfully）
 * 3. 更新指标值失败（updateIndicatorValue_shouldThrowExceptionWhenNotFound）
 *
 * 使用 Mockito 进行依赖模拟和行为验证，通过 @InjectMocks 和 @Mock 实现服务和仓库层的单元测试。
 */
package com.example.jpaspringboot.service.impl;

import com.example.jpaspringboot.entity.VisualIndicatorData;
import com.example.jpaspringboot.repository.VisualIndicatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VisualIndicatorServiceImplTest {

    @InjectMocks
    private VisualIndicatorServiceImpl service;

    @Mock
    private VisualIndicatorRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试方法：getAllCurrentData_shouldReturnList
     *
     * 测试场景：验证获取当前所有指标数据是否成功。
     * 步骤：
     * 1. 构建模拟数据对象，包含区域名称、指标键、值和年份。
     * 2. 使用 Mockito 模拟仓库层返回数据。
     * 3. 调用 getAllCurrentData 方法。
     * 4. 验证返回列表不为空，且包含预期的区域名称和指标键。
     * 5. 验证 findByYear 方法在仓库层被调用一次。
     */
    @Test
    void getAllCurrentData_shouldReturnList() {
        VisualIndicatorData mockData = new VisualIndicatorData();
        mockData.setRegionName("琼海");
        mockData.setIndicatorKey("SOC");
        mockData.setValue(25.0);
        mockData.setYear(2024);

        when(repository.findByYear(2024)).thenReturn(Arrays.asList(mockData));

        List<VisualIndicatorData> result = service.getAllCurrentData();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("琼海", result.get(0).getRegionName());
        verify(repository, times(1)).findByYear(2024);
    }

    /**
     * 测试方法：updateIndicatorValue_shouldUpdateSuccessfully
     *
     * 测试场景：验证更新指标值操作是否成功。
     * 步骤：
     * 1. 构建初始指标数据，并设置初始值和年份。
     * 2. 使用 Mockito 模拟 findByRegionNameAndIndicatorKeyAndYear 返回该数据。
     * 3. 模拟 save 方法成功更新数据。
     * 4. 调用 updateIndicatorValue 方法并验证返回值更新成功。
     * 5. 验证仓库层方法被正确调用。
     */
    @Test
    void updateIndicatorValue_shouldUpdateSuccessfully() {
        VisualIndicatorData data = new VisualIndicatorData();
        data.setRegionName("文昌");
        data.setIndicatorKey("CH4");
        data.setValue(10.0);
        data.setYear(2024);

        when(repository.findByRegionNameAndIndicatorKeyAndYear("文昌", "CH4", 2024)).thenReturn(data);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VisualIndicatorData updated = service.updateIndicatorValue("文昌", "CH4", 15.5);

        assertNotNull(updated);
        assertEquals(15.5, updated.getValue());
        verify(repository).findByRegionNameAndIndicatorKeyAndYear("文昌", "CH4", 2024);
        verify(repository).save(data);
    }

    /**
     * 测试方法：updateIndicatorValue_shouldThrowExceptionWhenNotFound
     *
     * 测试场景：验证更新指标值时，数据不存在的异常处理。
     * 步骤：
     * 1. 使用 Mockito 模拟 findByRegionNameAndIndicatorKeyAndYear 返回 null。
     * 2. 调用 updateIndicatorValue 方法，传入不存在的区域和指标键。
     * 3. 验证方法抛出 RuntimeException 异常，异常信息包含 "指定地区或指标不存在"。
     * 4. 验证 save 方法从未被调用。
     */
    @Test
    void updateIndicatorValue_shouldThrowExceptionWhenNotFound() {
        when(repository.findByRegionNameAndIndicatorKeyAndYear("三亚", "CO2", 2024)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.updateIndicatorValue("三亚", "CO2", 20.0);
        });

        assertTrue(ex.getMessage().contains("指定地区或指标不存在"));
        verify(repository).findByRegionNameAndIndicatorKeyAndYear("三亚", "CO2", 2024);
        verify(repository, never()).save(any());
    }
}
