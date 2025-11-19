/**
 * 区域多维碳指标查询服务接口
 *
 * 功能概述：
 * • 定义区域碳指标数据查询的契约规范
 * • 提供按行政区划名称获取碳汇指标的方法
 * • 服务于碳汇可视化分析的数据获取需求
 *
 * 业务场景：
 * • 区域碳汇能力评估与对比分析
 * • 行政区划维度的碳指标数据展示
 * • 碳汇可视化大屏数据支撑服务
 *
 * 设计规范：
 * • 接口层与实现层分离，支持多数据源适配
 * • 方法命名遵循Spring Data查询规范
 * • 返回实体对象包含区域多维碳指标数据
 *
 */
package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionMultiCarbonMetrics;

public interface RegionMultiCarbonMetricsService {

    /**
     * 根据行政区划名称查询多维碳指标数据
     *
     * @param regionName 行政区划名称（如：海南省、广州市、三沙市）
     * @return 区域多维碳指标实体对象，包含碳汇总量、构成、趋势等数据
     */
    RegionMultiCarbonMetrics getByRegionName(String regionName);
}