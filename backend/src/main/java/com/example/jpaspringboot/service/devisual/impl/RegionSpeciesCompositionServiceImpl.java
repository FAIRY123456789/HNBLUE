/**
 * 区域物种组成服务实现类
 *
 * 功能概述：
 * • 提供区域物种组成数据的业务逻辑处理
 * • 封装数据访问层操作，提供区域维度的物种查询服务
 *
 * 业务职责：
 * • 按区域名称查询物种组成信息
 * • 维护业务逻辑与数据访问的分离架构
 *
 * 技术实现：
 * • 基于Spring Service组件实现依赖注入
 * • 通过JPA Repository进行数据持久化操作
 * • 遵循接口分离原则提供服务契约
 */
package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionSpeciesComposition;
import com.example.jpaspringboot.repository.devisual.RegionSpeciesCompositionRepository;
import com.example.jpaspringboot.service.devisual.RegionSpeciesCompositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionSpeciesCompositionServiceImpl implements RegionSpeciesCompositionService {

    @Autowired
    private RegionSpeciesCompositionRepository repository;

    /**
     * 根据区域名称查询物种组成列表
     *
     * @param regionName 区域名称查询条件
     * @return 该区域下的物种组成数据列表
     */
    @Override
    public List<RegionSpeciesComposition> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}