/**
 * 区域物种组成数据访问层接口
 *
 * 功能概述：
 * • 提供区域物种组成数据的持久化操作接口
 * • 支持按区域名称查询物种组成信息
 * • 继承JPA标准接口获得基础CRUD操作能力
 *
 * 数据实体：
 * • RegionSpeciesComposition - 区域物种组成实体
 * • 主键类型：Integer
 *
 * 核心方法：
 * • 继承方法：save, findById, findAll, delete等
 * • 自定义方法：findByRegionName - 按区域名称查询
 *
 * 查询场景：
 * • 获取特定区域的物种组成清单
 * • 统计分析区域生物多样性
 * • 区域生态评估数据支撑
 *
 * 技术特性：
 * • 基于Spring Data JPA查询派生机制
 * • 自动生成SQL查询语句
 * • 支持分页和排序扩展
 */
package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionSpeciesComposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionSpeciesCompositionRepository extends JpaRepository<RegionSpeciesComposition, Integer> {

    /**
     * 根据区域名称查询物种组成列表
     *
     * @param regionName 区域名称
     * @return 该区域的物种组成列表，如果不存在返回空列表
     */
    List<RegionSpeciesComposition> findByRegionName(String regionName);
}