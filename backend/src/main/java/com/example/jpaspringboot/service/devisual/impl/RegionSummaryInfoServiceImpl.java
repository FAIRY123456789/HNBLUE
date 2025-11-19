/**
 * 区域汇总信息服务实现类
 *
 * 功能概述：
 * • 提供区域汇总信息的业务逻辑处理
 * • 封装数据访问层操作，实现服务接口契约
 * • 作为控制器与仓储层之间的业务协调层
 *
 * 业务职责：
 * • 按区域名称查询区域汇总信息
 * • 处理区域数据查询的业务逻辑
 * • 提供区域维度数据聚合服务
 *
 * 技术实现：
 * • 基于Spring Service组件架构
 * • 依赖注入数据访问层Repository
 * • 实现服务接口确保契约一致性
 *
 * 数据流：
 * • 控制器层 → 服务实现层 → 数据仓储层 → 数据库
 * • 返回区域汇总实体对象供上层使用
 */
package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionSummaryInfo;
import com.example.jpaspringboot.repository.devisual.RegionSummaryInfoRepository;
import com.example.jpaspringboot.service.devisual.RegionSummaryInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegionSummaryInfoServiceImpl implements RegionSummaryInfoService {

    @Autowired
    private RegionSummaryInfoRepository repository;

    /**
     * 根据区域名称查询区域汇总信息
     *
     * @param regionName 区域名称查询条件
     * @return RegionSummaryInfo 区域汇总信息实体对象
     *         包含该区域的统计摘要和关键指标数据
     */
    @Override
    public RegionSummaryInfo getByRegionName(String regionName) {
        // 调用数据访问层，按区域名称查询区域汇总信息
        return repository.findByRegionName(regionName);
    }
}