/**
 * 区域通量因子服务接口
 *
 * 功能概述：
 * • 定义区域通量因子数据的业务操作契约
 * • 提供按区域名称查询通量因子的标准接口
 *
 * 核心职责：
 * • 区域通量因子数据检索
 * • 区域维度数据聚合查询
 *
 * 业务场景：
 * • 碳排放计算中的区域特征因子获取
 * • 地理空间数据分析的区域维度查询
 * • 环境监测数据的区域统计展示
 *
 * 数据实体：
 * • RegionFluxFactor - 区域通量因子实体，包含区域特征参数
 *
 * 扩展方向：
 * • 支持多区域批量查询
 * • 添加时间维度过滤条件
 * • 集成区域空间关系查询
 */
package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionFluxFactor;

import java.util.List;

public interface RegionFluxFactorService {

    /**
     * 根据区域名称查询通量因子数据
     *
     * @param regionName 区域名称标识
     * @return 区域通量因子数据列表，包含该区域的所有相关因子记录
     */
    List<RegionFluxFactor> getByRegionName(String regionName);
}