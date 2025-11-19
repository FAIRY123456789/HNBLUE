/**
 * 区域生态摘要信息实体类
 *
 * 功能概述：
 * • 映射区域生态统计摘要数据到数据库表
 * • 存储红树林区域关键生态指标信息
 * • 支持生态数据可视化展示和分析
 *
 * 数据库映射：
 * • 对应表名：region_summary_info
 * • 主键策略：自增ID（GenerationType.IDENTITY）
 *
 * 核心字段说明：
 * • regionName - 区域名称标识
 * • dominantSpecies - 优势物种组成
 * • mangroveArea - 红树林面积（公顷）
 * • forestAge - 林分年龄（年）
 * • cnRatio - 碳氮比生态指标
 *
 * 应用场景：
 * • 区域生态概况数据展示
 * • 红树林分布统计报表
 * • 生态指标趋势分析
 * • 碳汇能力评估参考
 */
package com.example.jpaspringboot.entity.devisual;

import jakarta.persistence.*;

@Entity
@Table(name = "region_summary_info")
public class RegionSummaryInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String regionName;
    private String dominantSpecies;
    private Double mangroveArea;
    private Integer forestAge;
    private Double cnRatio;

    public RegionSummaryInfo() {
    }

    public RegionSummaryInfo(Integer id, String regionName, String dominantSpecies, Double mangroveArea, Integer forestAge, Double cnRatio) {
        this.id = id;
        this.regionName = regionName;
        this.dominantSpecies = dominantSpecies;
        this.mangroveArea = mangroveArea;
        this.forestAge = forestAge;
        this.cnRatio = cnRatio;
    }

    /**
     * 获取
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取
     * @return regionName
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * 设置
     * @param regionName
     */
    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    /**
     * 获取
     * @return dominantSpecies
     */
    public String getDominantSpecies() {
        return dominantSpecies;
    }

    /**
     * 设置
     * @param dominantSpecies
     */
    public void setDominantSpecies(String dominantSpecies) {
        this.dominantSpecies = dominantSpecies;
    }

    /**
     * 获取
     * @return mangroveArea
     */
    public Double getMangroveArea() {
        return mangroveArea;
    }

    /**
     * 设置
     * @param mangroveArea
     */
    public void setMangroveArea(Double mangroveArea) {
        this.mangroveArea = mangroveArea;
    }

    /**
     * 获取
     * @return forestAge
     */
    public Integer getForestAge() {
        return forestAge;
    }

    /**
     * 设置
     * @param forestAge
     */
    public void setForestAge(Integer forestAge) {
        this.forestAge = forestAge;
    }

    /**
     * 获取
     * @return cnRatio
     */
    public Double getCnRatio() {
        return cnRatio;
    }

    /**
     * 设置
     * @param cnRatio
     */
    public void setCnRatio(Double cnRatio) {
        this.cnRatio = cnRatio;
    }

    public String toString() {
        return "RegionSummaryInfo{id = " + id + ", regionName = " + regionName + ", dominantSpecies = " + dominantSpecies + ", mangroveArea = " + mangroveArea + ", forestAge = " + forestAge + ", cnRatio = " + cnRatio + "}";
    }

}

