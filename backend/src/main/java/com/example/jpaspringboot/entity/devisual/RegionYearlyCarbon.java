/**
 * 区域年度碳储量实体类
 *
 * 功能概述：
 * • 映射区域年度碳储量数据库表结构
 * • 记录各地区不同年份的碳储量和碳通量数据
 * • 支持碳循环分析和区域碳排放管理
 *
 * 数据库映射：
 * • 表名：region_yearly_carbon
 * • 唯一约束：regionName和year组合唯一
 * • 主键策略：自增ID
 *
 * 核心字段：
 * • regionName - 区域名称标识
 * • year - 统计年份
 * • carbonStorage - 碳储量（吨）
 * • carbonFlux - 碳通量（吨/年）
 *
 * 应用场景：
 * • 区域碳汇能力评估
 * • 碳排放趋势分析
 * • 碳中和目标跟踪
 * • 气候变化研究数据支撑
 */
package com.example.jpaspringboot.entity.devisual;
import jakarta.persistence.*;
@Entity
@Table(name = "region_yearly_carbon", uniqueConstraints = @UniqueConstraint(columnNames = {"regionName", "year"}))
public class RegionYearlyCarbon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String regionName;
    private Integer year;
    private Double carbonStorage;
    private Double carbonFlux;

    public RegionYearlyCarbon() {
    }

    public RegionYearlyCarbon(Integer id, String regionName, Integer year, Double carbonStorage, Double carbonFlux) {
        this.id = id;
        this.regionName = regionName;
        this.year = year;
        this.carbonStorage = carbonStorage;
        this.carbonFlux = carbonFlux;
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
     * @return year
     */
    public Integer getYear() {
        return year;
    }

    /**
     * 设置
     * @param year
     */
    public void setYear(Integer year) {
        this.year = year;
    }

    /**
     * 获取
     * @return carbonStorage
     */
    public Double getCarbonStorage() {
        return carbonStorage;
    }

    /**
     * 设置
     * @param carbonStorage
     */
    public void setCarbonStorage(Double carbonStorage) {
        this.carbonStorage = carbonStorage;
    }

    /**
     * 获取
     * @return carbonFlux
     */
    public Double getCarbonFlux() {
        return carbonFlux;
    }

    /**
     * 设置
     * @param carbonFlux
     */
    public void setCarbonFlux(Double carbonFlux) {
        this.carbonFlux = carbonFlux;
    }

    public String toString() {
        return "RegionYearlyCarbon{id = " + id + ", regionName = " + regionName + ", year = " + year + ", carbonStorage = " + carbonStorage + ", carbonFlux = " + carbonFlux + "}";
    }

    // Getters & Setters
}

