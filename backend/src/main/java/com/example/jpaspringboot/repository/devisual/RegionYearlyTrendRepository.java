/**
 * 区域年度趋势数据访问层接口
 *
 * 功能概述：
 * • 提供区域年度趋势数据的持久化操作接口
 * • 继承JPA标准接口获得基础CRUD操作能力
 * • 支持按区域名称查询年度趋势数据
 *
 * 数据实体：
 * • RegionYearlyTrend - 区域年度趋势统计实体
 * • 主键类型：Integer
 *
 * 查询方法：
 * • findByRegionName - 根据区域名称查询对应的年度趋势数据列表
 *
 * 技术特性：
 * • 基于Spring Data JPA的派生查询机制
 * • 自动生成SQL查询语句
 * • 支持分页和排序扩展
 *
 * 使用场景：
 * • 区域发展分析报告生成
 * • 年度趋势数据可视化
 * • 区域对比分析数据获取
 */
package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionYearlyTrend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionYearlyTrendRepository extends JpaRepository<RegionYearlyTrend, Integer> {

    /**
     * 根据区域名称查询年度趋势数据
     *
     * @param regionName 区域名称
     * @return 该区域的年度趋势数据列表，按时间排序
     */
    List<RegionYearlyTrend> findByRegionName(String regionName);
}