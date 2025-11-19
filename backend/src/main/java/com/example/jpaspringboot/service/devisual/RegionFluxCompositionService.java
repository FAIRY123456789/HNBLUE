/**
 * 区域通量组成服务接口
 *
 * 功能概述：
 * • 定义区域碳通量组成数据的查询服务契约
 * • 提供按区域名称获取通量组成信息的业务接口
 *
 * 核心职责：
 * • 区域碳通量数据检索
 * • 通量组成成分信息查询
 *
 * 业务场景：
 * • 区域碳循环分析
 * • 碳通量组成成分展示
 * • 区域生态碳汇能力评估
 *
 * 数据实体：
 * • RegionFluxComposition - 区域通量组成实体，包含各组分通量数据
 */
package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionFluxComposition;

import java.util.List;

public interface RegionFluxCompositionService {

    /**
     * 根据区域名称查询通量组成数据
     *
     * @param regionName 区域名称标识
     * @return 该区域的通量组成数据列表
     */
    List<RegionFluxComposition> getByRegionName(String regionName);
}