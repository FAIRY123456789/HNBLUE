/**
 * 区域多维碳指标数据服务实现类
 *
 * 功能概述：
 * • 提供区域碳指标数据的业务逻辑处理
 * • 封装数据访问层操作，提供统一的服务接口
 * • 实现按区域名称查询碳指标数据的核心业务功能
 *
 * 业务职责：
 * • 区域碳指标数据的检索与获取
 * • 业务逻辑与数据访问的分离处理
 * • 服务层事务边界管理
 *
 * 数据流：
 * • 控制器层 → 服务层 → 数据访问层 → 数据库
 * • 返回区域名称对应的完整碳指标实体数据
 *
 * 设计模式：
 * • 服务接口与实现分离
 * • 依赖注入数据访问组件
 * • 单一职责原则应用
 */
package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionMultiCarbonMetrics;
import com.example.jpaspringboot.repository.devisual.RegionMultiCarbonMetricsRepository;
import com.example.jpaspringboot.service.devisual.RegionMultiCarbonMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegionMultiCarbonMetricsServiceImpl implements RegionMultiCarbonMetricsService {

    @Autowired
    private RegionMultiCarbonMetricsRepository repository;

    /**
     * 根据区域名称查询多维碳指标数据
     *
     * @param regionName 区域名称查询条件
     * @return RegionMultiCarbonMetrics 区域碳指标实体对象
     *         包含该区域的碳排放、碳汇等多维度指标数据
     */
    @Override
    public RegionMultiCarbonMetrics getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}