/**
 * 区域通量因子数据访问层接口
 *
 * 功能概述：
 * • 提供区域通量因子实体的基础CRUD操作
 * • 支持按区域名称查询相关通量因子数据
 * • 继承JPA标准接口获得数据访问能力
 *
 * 数据实体：
 * • RegionFluxFactor - 区域通量因子实体，存储区域相关的通量计算参数
 *
 * 核心方法：
 * • 继承方法 - save(), findById(), findAll(), deleteById() 等
 * • 自定义查询 - findByRegionName() 按区域名称筛选记录
 *
 * 查询特性：
 * • 方法名派生查询 - 根据方法名自动生成JPQL查询
 * • 返回列表结果 - 支持同一区域多个通量因子配置
 * • 空安全处理 - 返回空列表而非null值
 *
 * 使用场景：
 * • 碳通量计算参数管理
 * • 区域生态数据分析
 * • 环境监测系统数据支撑
 */
package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionFluxFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionFluxFactorRepository extends JpaRepository<RegionFluxFactor, Integer> {

    /**
     * 根据区域名称查询通量因子配置列表
     *
     * @param regionName 区域名称
     * @return 该区域对应的通量因子配置列表，无结果时返回空列表
     */
    List<RegionFluxFactor> findByRegionName(String regionName);
}