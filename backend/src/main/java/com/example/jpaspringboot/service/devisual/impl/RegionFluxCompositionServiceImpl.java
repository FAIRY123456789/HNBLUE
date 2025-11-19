/**
 * 区域通量组成服务实现类
 *
 * 功能概述：
 * • 提供区域通量组成数据的业务逻辑处理
 * • 封装数据访问层操作，提供区域维度的数据查询
 * • 作为控制器与仓储层之间的业务逻辑桥梁
 *
 * 业务职责：
 * • 按区域名称查询通量组成数据
 * • 实现区域维度数据分析的业务规则
 * • 提供区域碳通量组成的统计查询
 *
 * 技术实现：
 * • 基于Spring Service注解声明服务组件
 * • 依赖注入RegionFluxCompositionRepository数据访问接口
 * • 实现RegionFluxCompositionService业务接口契约
 *
 * 数据流向：
 * 控制器层 → 服务实现层 → 数据仓储层 → 数据库
 */
package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionFluxComposition;
import com.example.jpaspringboot.repository.devisual.RegionFluxCompositionRepository;
import com.example.jpaspringboot.service.devisual.RegionFluxCompositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionFluxCompositionServiceImpl implements RegionFluxCompositionService {

    @Autowired
    private RegionFluxCompositionRepository repository;  // 区域通量组成数据访问接口

    /**
     * 根据区域名称查询通量组成数据
     *
     * @param regionName 区域名称
     * @return 区域通量组成数据列表
     */
    @Override
    public List<RegionFluxComposition> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);  // 调用数据访问层查询方法
    }
}