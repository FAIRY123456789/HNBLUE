/**
 * 可视化指标数据服务接口
 *
 * 功能概述：
 * • 提供区域可视化指标数据的查询与更新服务
 * • 管理各类关键绩效指标(KPI)的当前状态数据
 * • 支持指标数据的实时更新和状态追踪
 *
 * 核心操作：
 * • 获取所有区域的当前指标数据快照
 * • 按区域和指标键更新特定指标数值
 *
 * 业务场景：
 * • 区域发展指标监控大屏
 * • 关键绩效指标(KPI)动态展示
 * • 实时数据看板与决策支持
 *
 * 数据模型：
 * • VisualIndicatorData - 可视化指标数据实体
 * • regionName - 区域标识
 * • indicatorKey - 指标类型键
 * • value - 指标数值
 */
package com.example.jpaspringboot.service;
import com.example.jpaspringboot.entity.VisualIndicatorData;

import java.util.List;

public interface VisualIndicatorService {

    /**
     * 获取所有区域的当前可视化指标数据
     * @return 指标数据列表，包含各区域各项指标的当前状态
     */
    List<VisualIndicatorData> getAllCurrentData();

    /**
     * 更新指定区域和指标键的指标数值
     * @param regionName 区域名称标识
     * @param indicatorKey 指标类型键
     * @param value 新的指标数值
     * @return 更新后的可视化指标数据实体
     */
    VisualIndicatorData updateIndicatorValue(String regionName, String indicatorKey, double value);
}