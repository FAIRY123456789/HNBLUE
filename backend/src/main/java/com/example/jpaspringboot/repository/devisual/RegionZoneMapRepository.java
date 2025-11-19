/**
 * 行政区划与气候带映射数据访问层接口
 *
 * 功能概述：
 * • 提供行政区划名称与气候带编码的映射关系数据访问
 * • 基于JPA规范实现基础CRUD操作
 * • 支持按行政区划名称查询气候带信息
 *
 * 实体关系：
 * • 主键类型：String（行政区划编码或名称）
 * • 关联实体：RegionZoneMap（行政区划-气候带映射实体）
 *
 * 核心方法：
 * • 继承JpaRepository获得标准CRUD操作
 * • findByRegionName - 根据行政区划名称查询气候带映射
 *
 * 使用场景：
 * • 地理信息系统中的区域气候分类
 * • 生态研究中的气候带划分查询
 * • 农业规划中的区域气候特征分析
 *
 * 查询特性：
 * • 方法名派生查询，自动生成查询逻辑
 * • 支持行政区划名称精确匹配查询
 * • 返回完整的区域-气候带映射信息
 */
package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionZoneMap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionZoneMapRepository extends JpaRepository<RegionZoneMap, String> {

    /**
     * 根据行政区划名称查询气候带映射信息
     *
     * @param regionName 行政区划名称
     * @return 区域气候带映射实体，包含气候带编码和描述信息
     */
    RegionZoneMap findByRegionName(String regionName);
}