/**
 * 区域年度碳储量数据服务实现类
 *
 * 功能概述：
 * • 提供区域年度碳储量数据的业务逻辑处理
 * • 封装数据访问层操作，提供区域维度的碳储量查询
 *
 * 服务职责：
 * • 按区域名称查询年度碳储量数据
 * • 业务逻辑与数据访问的分离桥接
 *
 * 数据流向：
 * • 控制器层 → 服务层 → 数据仓库层 → 数据库
 *
 * 使用场景：
 * • 区域碳汇能力分析报表
 * • 年度碳储量变化趋势展示
 * • 区域间碳汇对比分析
 */
package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionYearlyCarbon;
import com.example.jpaspringboot.repository.devisual.RegionYearlyCarbonRepository;
import com.example.jpaspringboot.service.devisual.RegionYearlyCarbonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionYearlyCarbonServiceImpl implements RegionYearlyCarbonService {

    @Autowired
    private RegionYearlyCarbonRepository repository;

    /**
     * 根据区域名称查询年度碳储量数据
     *
     * @param regionName 区域名称
     * @return 该区域的年度碳储量数据列表
     */
    @Override
    public List<RegionYearlyCarbon> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}