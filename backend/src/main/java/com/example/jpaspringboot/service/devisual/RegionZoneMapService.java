/**
 * 行政区划区域映射服务接口
 *
 * 功能概述：
 * • 提供行政区划名称与区域编码的映射查询服务
 * • 支持区域信息的快速检索和访问
 *
 * 业务场景：
 * • 地理信息系统中的区域编码解析
 * • 行政区划数据的标准化查询
 * • 区域名称与编码的映射关系管理
 *
 * 设计规范：
 * • 接口层定义服务契约，明确业务边界
 * • 支持区域名称的精确匹配查询
 * • 返回完整的区域映射实体信息
 */
package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionZoneMap;

public interface RegionZoneMapService {

    /**
     * 根据行政区划名称查询区域映射信息
     *
     * @param regionName 行政区划名称（如：海南省、北京市等）
     * @return 区域映射实体对象，包含区域编码等详细信息
     */
    RegionZoneMap getByRegionName(String regionName);
}