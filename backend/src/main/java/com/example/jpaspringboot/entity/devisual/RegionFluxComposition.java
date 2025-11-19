/**
 * 区域温室气体通量组成实体类
 *
 * 功能概述：
 * • 映射区域温室气体排放组成数据的数据库表结构
 * • 记录不同地区各年份的温室气体通量成分
 * • 提供温室气体排放数据的持久化存储模型
 *
 * 数据库映射：
 * • 表名：region_flux_composition
 * • 唯一约束：region_name + year 组合唯一索引
 * • 主键策略：自增ID
 *
 * 数据字段说明：
 * • CO2 - 二氧化碳通量排放量
 * • CH4 - 甲烷通量排放量
 * • N2O - 氧化亚氮通量排放量
 * • GHG - 温室气体总通量排放量
 *
 * 应用场景：
 * • 区域碳排放清单管理
 * • 温室气体成分分析
 * • 气候变化研究数据支撑
 * • 环境监测报告生成
 */
package com.example.jpaspringboot.entity.devisual;
import jakarta.persistence.*;
@Entity
@Table(
        name = "region_flux_composition",
        uniqueConstraints = @UniqueConstraint(columnNames = {"region_name", "year"})
)
public class RegionFluxComposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "year")
    private Integer year;

    @Column(name = "co2")
    private Double co2;

    @Column(name = "ch4")
    private Double ch4;

    @Column(name = "n2o")
    private Double n2o;

    @Column(name = "ghg")
    private Double ghg;

    public RegionFluxComposition() {
    }

    public RegionFluxComposition(Integer id, String regionName, Integer year, Double co2, Double ch4, Double n2o, Double ghg) {
        this.id = id;
        this.regionName = regionName;
        this.year = year;
        this.co2 = co2;
        this.ch4 = ch4;
        this.n2o = n2o;
        this.ghg = ghg;
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
     * @return co2
     */
    public Double getCo2() {
        return co2;
    }

    /**
     * 设置
     * @param co2
     */
    public void setCo2(Double co2) {
        this.co2 = co2;
    }

    /**
     * 获取
     * @return ch4
     */
    public Double getCh4() {
        return ch4;
    }

    /**
     * 设置
     * @param ch4
     */
    public void setCh4(Double ch4) {
        this.ch4 = ch4;
    }

    /**
     * 获取
     * @return n2o
     */
    public Double getN2o() {
        return n2o;
    }

    /**
     * 设置
     * @param n2o
     */
    public void setN2o(Double n2o) {
        this.n2o = n2o;
    }

    /**
     * 获取
     * @return ghg
     */
    public Double getGhg() {
        return ghg;
    }

    /**
     * 设置
     * @param ghg
     */
    public void setGhg(Double ghg) {
        this.ghg = ghg;
    }

    public String toString() {
        return "RegionFluxComposition{id = " + id + ", regionName = " + regionName + ", year = " + year + ", co2 = " + co2 + ", ch4 = " + ch4 + ", n2o = " + n2o + ", ghg = " + ghg + "}";
    }

}
