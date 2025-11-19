/**
 * 区域多维碳指标数据访问层接口
 *
 * 功能概述：
 * • 提供区域碳指标数据的持久化操作接口
 * • 基于区域名称快速查询碳计量指标数据
 * • 继承JPA标准接口实现基础CRUD操作
 *
 * 数据实体：
 * • RegionMultiCarbonMetrics - 区域多维碳计量指标实体
 * • 主键类型：Integer（区域ID或自增主键）
 *
 * 核心方法：
 * • 继承方法：save, findById, findAll, delete等标准CRUD操作
 * • 自定义方法：findByRegionName - 按区域名称精确查询
 *
 * 查询特性：
 * • 基于Spring Data JPA方法命名规范自动生成查询
 * • 区域名称查询支持快速区域碳数据检索
 * • 返回单个实体对象，确保区域名称唯一性
 *
 * 使用场景：
 * • 区域碳汇能力分析数据查询
 * • 碳计量指标数据管理
 * • 区域生态评估数据获取
 */
package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionMultiCarbonMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionMultiCarbonMetricsRepository extends JpaRepository<RegionMultiCarbonMetrics, Integer> {

    /**
     * 根据区域名称查询碳指标数据
     *
     * @param regionName 区域名称（精确匹配）
     * @return 区域碳指标实体对象，未找到时返回null
     */
    RegionMultiCarbonMetrics findByRegionName(String regionName);
}