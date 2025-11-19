/**
 * 区域-地带映射关系服务实现类
 *
 * 功能概述：
 * • 提供区域名称到地理地带的映射查询服务
 * • 封装数据访问层，实现业务逻辑处理
 * • 服务于气候带划分、生态区域识别等场景
 *
 * 业务场景：
 * • 根据行政区域名称获取对应的生态地带信息
 * • 支持碳储量计算中的区域气候因子匹配
 * • 为生态模型提供地理分区数据支撑
 *
 * 数据关系：
 * • 区域名称（regionName）→ 地理地带信息（RegionZoneMap）
 * • 一对一的映射关系，每个区域对应唯一的地带配置
 */
package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionZoneMap;
import com.example.jpaspringboot.repository.devisual.RegionZoneMapRepository;
import com.example.jpaspringboot.service.devisual.RegionZoneMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegionZoneMapServiceImpl implements RegionZoneMapService {

    @Autowired
    private RegionZoneMapRepository repository;

    /**
     * 根据区域名称查询对应的地理地带映射信息
     *
     * @param regionName 区域名称（如省份、行政区名称）
     * @return RegionZoneMap 区域地带映射实体，包含气候带、生态区等信息
     */
    @Override
    public RegionZoneMap getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}