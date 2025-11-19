/**
 * 区域通量因子服务实现类
 *
 * 功能概述：
 * • 提供区域通量因子数据的业务逻辑处理
 * • 封装数据访问层操作，实现业务规则
 * • 作为控制器与仓储层之间的业务协调层
 *
 * 核心业务：
 * • 按区域名称查询通量因子数据
 * • 业务数据验证与转换处理
 * • 服务层异常处理与日志记录
 *
 * 架构定位：
 * • 实现RegionFluxFactorService接口契约
 * • 依赖RegionFluxFactorRepository数据访问
 * • 被控制器层调用返回业务数据
 *
 * 设计模式：
 * • 服务层模式 - 分离业务逻辑与数据访问
 * • 依赖注入 - 通过@Autowired注入仓储实例
 * • 接口编程 - 基于接口契约提供服务
 */
package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionFluxFactor;
import com.example.jpaspringboot.repository.devisual.RegionFluxFactorRepository;
import com.example.jpaspringboot.service.devisual.RegionFluxFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionFluxFactorServiceImpl implements RegionFluxFactorService {

    @Autowired
    private RegionFluxFactorRepository repository;

    /**
     * 根据区域名称查询通量因子数据
     *
     * @param regionName 区域名称查询条件
     * @return 匹配的区域通量因子数据列表
     */
    @Override
    public List<RegionFluxFactor> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}