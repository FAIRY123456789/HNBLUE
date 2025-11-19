/**
 * 区域经济数据投影仓库接口
 *
 * 功能概述：
 * • 提供区域经济投影数据的持久化操作接口
 * • 支持基于区域名称的数据查询功能
 * • 继承JPA标准仓库接口获得基础CRUD能力
 *
 * 数据特性：
 * • 实体类型：RegionEconomyProjection（区域经济投影实体）
 * • 主键类型：Integer（整型标识符）
 *
 * 查询方法：
 * • findByRegionName - 根据区域名称查询经济投影数据
 *
 * 继承功能：
 * • save() / saveAll() - 实体保存操作
 * • findById() - 根据ID查询单个实体
 * • findAll() - 查询所有实体列表
 * • delete() / deleteAll() - 实体删除操作
 *
 * 使用场景：
 * • 区域经济指标数据检索
 * • 经济数据分析与可视化
 * • 区域发展对比研究
 */
package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionEconomyProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionEconomyProjectionRepository extends JpaRepository<RegionEconomyProjection, Integer> {

    /**
     * 根据区域名称查询经济投影数据
     *
     * @param regionName 区域名称
     * @return 匹配的区域经济投影数据列表
     */
    List<RegionEconomyProjection> findByRegionName(String regionName);
}