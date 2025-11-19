/**
 * 区域物种组成服务接口
 *
 * 功能概述：
 * • 提供按区域名称查询物种组成数据的业务逻辑抽象
 * • 定义区域生态数据分析的核心查询契约
 *
 * 业务场景：
 * • 区域生物多样性统计分析
 * • 生态保护区物种组成监测
 * • 地理分布特征数据查询
 *
 * 数据实体：
 * • RegionSpeciesComposition - 区域物种组成实体，包含物种分布和数量信息
 *
 * 设计规范：
 * • 接口层隔离业务逻辑与数据访问细节
 * • 支持未来多种数据源实现（数据库、缓存、外部API）
 * • 为区域生态研究提供统一的数据访问入口
 */
package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionSpeciesComposition;
import java.util.List;

public interface RegionSpeciesCompositionService {

    /**
     * 根据区域名称查询物种组成数据
     *
     * @param regionName 区域名称（如保护区名称、行政区划名称等）
     * @return 该区域的物种组成列表，包含各物种的分布和数量信息
     */
    List<RegionSpeciesComposition> getByRegionName(String regionName);
}