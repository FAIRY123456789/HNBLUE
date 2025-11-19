/**
 * 可视化指标数据仓库接口
 *
 * 功能概述：
 * • 提供可视化指标数据的持久化操作接口
 * • 支持按年份、地区、指标键等多维度查询
 * • 继承JPA标准接口获得基础CRUD操作能力
 *
 * 数据实体：
 * • VisualIndicatorData - 可视化指标数据实体
 * • 主键类型：Long
 *
 * 自定义查询方法：
 * • findByYear - 按年份查询指标数据列表
 * • findByRegionNameAndIndicatorKeyAndYear - 多条件精确查询单条指标数据
 *
 * 查询场景：
 * • 年度数据统计展示
 * • 地区指标趋势分析  
 * • 多维度数据对比
 *
 * 技术特性：
 * • 基于Spring Data JPA方法命名约定
 * • 自动生成SQL查询语句
 * • 支持分页和排序扩展
 */
package com.example.jpaspringboot.repository;
import com.example.jpaspringboot.entity.VisualIndicatorData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisualIndicatorRepository extends JpaRepository<VisualIndicatorData, Long> {

    /**
     * 根据年份查询可视化指标数据列表
     * @param year 查询年份
     * @return 对应年份的所有指标数据列表
     */
    List<VisualIndicatorData> findByYear(int year);

    /**
     * 多条件精确查询可视化指标数据
     * @param regionName 地区名称
     * @param indicatorKey 指标键名
     * @param year 数据年份
     * @return 匹配的单条指标数据记录
     */
    VisualIndicatorData findByRegionNameAndIndicatorKeyAndYear(String regionName, String indicatorKey, int year);
}