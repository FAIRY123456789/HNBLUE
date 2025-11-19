/**
 * 区域汇总信息服务接口
 *
 * 功能概述：
 * • 定义区域维度数据查询的契约接口
 * • 提供按区域名称获取汇总信息的标准方法
 *
 * 业务场景：
 * • 区域统计数据分析
 * • 地理信息聚合查询
 * • 管理单元数据汇总
 *
 * 设计原则：
 * • 单一职责原则 - 专注于区域维度数据查询
 * • 接口隔离原则 - 最小化接口依赖
 * • 面向接口编程 - 支持多种实现方式
 *
 * 实现要求：
 * • 区域名称作为唯一查询条件
 * • 返回完整的区域汇总信息实体
 * • 支持空值处理和异常管理
 */
package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionSummaryInfo;

public interface RegionSummaryInfoService {

    /**
     * 根据区域名称查询区域汇总信息
     *
     * @param regionName 区域名称，作为查询条件
     * @return 区域汇总信息实体，包含该区域的统计数据和摘要信息
     */
    RegionSummaryInfo getByRegionName(String regionName);
}