/**
 * 区域汇总信息数据访问层接口
 *
 * 功能概述：
 * • 提供区域汇总信息的数据库操作接口
 * • 继承JPA标准仓库接口获得基础CRUD能力
 * • 支持按区域名称查询特定区域汇总信息
 *
 * 数据实体：
 * • RegionSummaryInfo - 区域汇总信息实体类
 * • 主键类型：Integer
 *
 * 核心方法：
 * • 继承方法：save, findById, findAll, delete等
 * • 自定义方法：findByRegionName - 按区域名称精确查询
 *
 * 使用场景：
 * • 区域数据统计分析
 * • 地理信息管理系统
 * • 区域指标数据查询
 */
package com.example.jpaspringboot.repository.devisual;
import com.example.jpaspringboot.entity.devisual.RegionSummaryInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionSummaryInfoRepository extends JpaRepository<RegionSummaryInfo, Integer> {

    /**
     * 根据区域名称查询区域汇总信息
     *
     * @param regionName 区域名称
     * @return 区域汇总信息实体，未找到时返回null
     */
    RegionSummaryInfo findByRegionName(String regionName);
}