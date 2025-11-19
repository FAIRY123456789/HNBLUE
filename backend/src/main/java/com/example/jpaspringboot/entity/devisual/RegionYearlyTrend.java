/**
 * 区域年度趋势数据实体类
 *
 * 功能概述：
 * • 映射区域年度碳循环指标数据的数据库表结构
 * • 记录不同地区逐年变化的生态碳汇关键参数
 * • 支持区域碳储量动态分析和趋势预测
 *
 * 数据指标说明：
 * • SOC - 土壤有机碳储量 (Soil Organic Carbon)
 * • Biomass - 生物量碳储量
 * • CO2 Flux - 二氧化碳通量（碳吸收/释放）
 * • CH4 Flux - 甲烷通量（温室气体排放）
 *
 * 数据库约束：
 * • 区域名称和年份组合唯一索引，避免重复数据
 * • 自增主键确保数据记录唯一性
 *
 * 应用场景：
 * • 区域碳汇能力年度对比分析
 * • 碳循环动态监测数据存储
 * • 气候变化影响评估数据支撑
 */
package com.example.jpaspringboot.entity.devisual;
import jakarta.persistence.*;
@Entity
@Table(name = "region_yearly_trend", uniqueConstraints = @UniqueConstraint(columnNames = {"region_name", "year"}))
public class RegionYearlyTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "year")
    private Integer year;

    @Column(name = "soc")
    private Double soc;

    @Column(name = "biomass")
    private Double biomass;

    @Column(name = "co2_flux")
    private Double co2Flux;

    @Column(name = "ch4_flux")
    private Double ch4Flux;
    public RegionYearlyTrend() {
    }

    public RegionYearlyTrend(Integer id, String regionName, Integer year, Double soc, Double biomass, Double co2Flux, Double ch4Flux) {
        this.id = id;
        this.regionName = regionName;
        this.year = year;
        this.soc = soc;
        this.biomass = biomass;
        this.co2Flux = co2Flux;
        this.ch4Flux = ch4Flux;
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
     * @return soc
     */
    public Double getSoc() {
        return soc;
    }

    /**
     * 设置
     * @param soc
     */
    public void setSoc(Double soc) {
        this.soc = soc;
    }

    /**
     * 获取
     * @return biomass
     */
    public Double getBiomass() {
        return biomass;
    }

    /**
     * 设置
     * @param biomass
     */
    public void setBiomass(Double biomass) {
        this.biomass = biomass;
    }

    /**
     * 获取
     * @return co2Flux
     */
    public Double getCo2Flux() {
        return co2Flux;
    }

    /**
     * 设置
     * @param co2Flux
     */
    public void setCo2Flux(Double co2Flux) {
        this.co2Flux = co2Flux;
    }

    /**
     * 获取
     * @return ch4Flux
     */
    public Double getCh4Flux() {
        return ch4Flux;
    }

    /**
     * 设置
     * @param ch4Flux
     */
    public void setCh4Flux(Double ch4Flux) {
        this.ch4Flux = ch4Flux;
    }

    public String toString() {
        return "RegionYearlyTrend{id = " + id + ", regionName = " + regionName + ", year = " + year + ", soc = " + soc + ", biomass = " + biomass + ", co2Flux = " + co2Flux + ", ch4Flux = " + ch4Flux + "}";
    }

    // Getters & Setters
}

