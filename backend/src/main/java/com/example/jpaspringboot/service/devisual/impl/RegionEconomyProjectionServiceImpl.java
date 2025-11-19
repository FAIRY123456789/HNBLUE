/**
 * 区域经济预测数据服务实现类
 *
 * 功能概述：
 * • 提供区域经济预测数据的业务逻辑处理
 * • 封装数据访问层操作，提供统一的服务接口
 * • 实现区域名称查询的区域经济预测数据检索
 *
 * 服务职责：
 * • 处理区域经济预测数据的查询业务逻辑
 * • 协调数据访问层与控制器层的交互
 * • 为上层业务模块提供经济数据查询能力
 *
 * 数据流向：
 * • 控制器层 → 服务层 → 数据访问层 → 数据库
 * • 返回区域经济指标预测结果列表
 *
 * 关联组件：
 * • RegionEconomyProjectionRepository - 区域经济数据访问接口
 * • RegionEconomyProjection - 区域经济预测实体类
 */
package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionEconomyProjection;
import com.example.jpaspringboot.repository.devisual.RegionEconomyProjectionRepository;
import com.example.jpaspringboot.service.devisual.RegionEconomyProjectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionEconomyProjectionServiceImpl implements RegionEconomyProjectionService {

    @Autowired
    private RegionEconomyProjectionRepository repository;

    /**
     * 根据区域名称查询经济预测数据
     *
     * @param regionName 区域名称查询条件
     * @return 区域经济预测数据列表，包含各项经济指标预测值
     */
    @Override
    public List<RegionEconomyProjection> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}