/**
 * 区域年度碳储量数据访问层接口
 *
 * 功能概述：
 * • 提供区域年度碳储量数据的持久化操作
 * • 支持基于区域名称的数据查询
 * • 继承JPA标准接口获得基础CRUD能力
 *
 * 数据实体：
 * • RegionYearlyCarbon - 区域年度碳储量统计实体
 * • 主键类型：Integer
 *
 * 查询方法：
 * • findByRegionName - 按区域名称查询碳储量记录
 *
 * 继承功能：
 * • save() / saveAll() - 保存实体
 * • findById() - 按ID查询
 * • findAll() - 查询所有记录  
 * • delete() / deleteAll() - 删除操作
 * • count() - 统计记录数
 *
 * 使用场景：
 * • 区域碳储量统计分析
 * • 年度碳排放趋势查询
 * • 地理区域数据聚合
 */
package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionYearlyCarbon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionYearlyCarbonRepository extends JpaRepository<RegionYearlyCarbon, Integer> {

    /**
     * 根据区域名称查询年度碳储量记录
     *
     * @param regionName 区域名称
     * @return 该区域的年度碳储量记录列表
     */
    List<RegionYearlyCarbon> findByRegionName(String regionName);
}