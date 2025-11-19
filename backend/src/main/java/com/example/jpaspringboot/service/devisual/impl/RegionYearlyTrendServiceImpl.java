/**
 * 区域年度趋势数据服务实现类
 *
 * 功能概述：
 * • 提供区域年度碳储量趋势数据的业务逻辑处理
 * • 封装数据访问层操作，提供区域维度的趋势查询
 *
 * 业务场景：
 * • 区域碳储量年度变化趋势分析
 * • 区域生态保护成效评估
 * • 碳排放政策效果监测
 *
 * 数据流向：
 * • 控制器层 → 服务层 → 数据访问层 → 数据库
 * • 返回区域维度的时间序列趋势数据
 */
package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionYearlyTrend;
import com.example.jpaspringboot.repository.devisual.RegionYearlyTrendRepository;
import com.example.jpaspringboot.service.devisual.RegionYearlyTrendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionYearlyTrendServiceImpl implements RegionYearlyTrendService {

    @Autowired
    private RegionYearlyTrendRepository repository;

    /**
     * 根据区域名称查询年度趋势数据
     *
     * @param regionName 区域名称
     * @return 该区域的年度趋势数据列表
     */
    @Override
    public List<RegionYearlyTrend> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}