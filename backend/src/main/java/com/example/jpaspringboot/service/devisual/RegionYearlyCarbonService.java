/**
 * 区域年度碳储量数据服务接口
 *
 * 功能概述：
 * • 提供区域维度年度碳储量数据的查询服务
 * • 支持按区域名称获取碳储量时间序列数据
 * • 为碳储量可视化分析提供数据支撑
 *
 * 业务场景：
 * • 区域碳汇能力趋势分析
 * • 年度碳储量变化监测
 * • 多区域碳汇对比分析
 *
 * 数据特性：
 * • 按区域名称进行数据聚合
 * • 包含年度时间序列维度
 * • 支持碳储量变化趋势分析
 *
 * 实现要求：
 * • 需实现区域数据的准确过滤
 * • 支持空结果集的合理处理
 * • 保证数据查询的性能效率
 */
package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionYearlyCarbon;
import java.util.List;

public interface RegionYearlyCarbonService {

    /**
     * 根据区域名称查询年度碳储量数据
     *
     * @param regionName 区域名称（如省份、城市等行政区域）
     * @return 该区域的年度碳储量数据列表，按年份排序
     */
    List<RegionYearlyCarbon> getByRegionName(String regionName);
}