/**
 * 区域通量组成数据访问层接口
 *
 * 功能概述：
 * • 提供区域通量组成实体的数据持久化操作
 * • 支持基于区域名称的查询功能
 * • 继承JPA标准接口获得基础CRUD操作能力
 *
 * 数据实体：
 * • RegionFluxComposition - 区域通量组成实体，映射数据库表结构
 *
 * 核心方法：
 * • 继承方法 - save(), findById(), findAll(), deleteById() 等
 * • 自定义查询 - findByRegionName() 按区域名称筛选记录
 *
 * 查询特性：
 * • 基于Spring Data JPA方法命名规范自动生成查询
 * • 返回区域名称匹配的所有记录列表
 * • 支持空结果集返回空列表
 *
 * 使用场景：
 * • 区域碳通量数据分析
 * • 地理空间数据查询
 * • 生态环境监测数据管理
 */
package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionFluxComposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionFluxCompositionRepository extends JpaRepository<RegionFluxComposition, Integer> {

    /**
     * 根据区域名称查询通量组成记录
     *
     * @param regionName 区域名称
     * @return 匹配的区域通量组成记录列表，按默认排序返回
     */
    List<RegionFluxComposition> findByRegionName(String regionName);
}