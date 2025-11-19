/**
 * 区域年度趋势数据服务接口
 *
 * 功能概述：
 * • 定义区域年度趋势数据的查询服务契约
 * • 提供按区域名称获取趋势数据的标准接口
 * • 支持区域维度的时间序列数据分析
 *
 * 业务场景：
 * • 区域发展态势监控与分析
 * • 年度对比趋势可视化
 * • 区域绩效指标追踪
 *
 * 数据维度：
 * • 区域维度 - 按行政区域或业务区域划分
 * • 时间维度 - 年度趋势数据序列
 * • 指标维度 - 多种业务指标的趋势变化
 *
 * 实现要求：
 * • 支持区域名称的精确匹配查询
 * • 返回按时间排序的完整趋势序列
 * • 保证数据的一致性和实时性
 */
package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionYearlyTrend;
import java.util.List;

public interface RegionYearlyTrendService {

    /**
     * 根据区域名称查询年度趋势数据
     *
     * @param regionName 区域名称（精确匹配）
     * @return 该区域的年度趋势数据列表，按时间顺序排列
     */
    List<RegionYearlyTrend> getByRegionName(String regionName);
}