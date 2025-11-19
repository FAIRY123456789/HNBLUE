/**
 * 区域经济数据投影查询服务接口
 *
 * 功能概述：
 * • 提供区域经济相关数据的投影查询功能
 * • 支持按区域名称筛选经济指标数据
 * • 定义数据访问层的服务契约
 *
 * 核心方法：
 * • getByRegionName - 根据区域名称查询经济投影数据
 *
 * 业务场景：
 * • 区域经济指标对比分析
 * • 区域发展态势数据展示
 * • 经济数据可视化支撑
 *
 */
package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionEconomyProjection;

import java.util.List;

public interface RegionEconomyProjectionService {

    /**
     * 根据区域名称查询经济投影数据
     *
     * @param regionName 区域名称（如省份、城市名称）
     * @return 区域经济投影数据列表，包含关键经济指标字段
     */
    List<RegionEconomyProjection> getByRegionName(String regionName);
}