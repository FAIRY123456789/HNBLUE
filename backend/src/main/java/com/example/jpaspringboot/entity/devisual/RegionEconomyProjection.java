/**
 * 区域经济预测数据实体类
 *
 * 功能概述：
 * • 映射区域经济预测数据的数据库表结构
 * • 维护不同情景下各区域的经济指标预测值
 * • 支持区域-情景组合的唯一性约束
 *
 * 数据库映射：
 * • 表名：region_economy_projection
 * • 唯一约束：region_name + scenario_name 组合唯一
 * • 主键策略：自增ID
 *
 * 业务含义：
 * • region_name - 区域名称标识
 * • scenario_name - 经济预测情景名称
 * • value - 该区域在特定情景下的经济指标数值
 *
 * 应用场景：
 * • 多情景经济预测分析
 * • 区域经济发展对比
 * • 政策影响模拟评估
 */
package com.example.jpaspringboot.entity.devisual;
import jakarta.persistence.*;

@Entity
@Table(name = "region_economy_projection",
        uniqueConstraints = @UniqueConstraint(columnNames = {"region_name", "scenario_name"}))
public class RegionEconomyProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "scenario_name")
    private String scenarioName;

    @Column(name = "value")
    private Double value;
    public RegionEconomyProjection() {
    }

    public RegionEconomyProjection(Integer id, String regionName, String scenarioName, Double value) {
        this.id = id;
        this.regionName = regionName;
        this.scenarioName = scenarioName;
        this.value = value;
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
     * @return scenarioName
     */
    public String getScenarioName() {
        return scenarioName;
    }

    /**
     * 设置
     * @param scenarioName
     */
    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    /**
     * 获取
     * @return value
     */
    public Double getValue() {
        return value;
    }

    /**
     * 设置
     * @param value
     */
    public void setValue(Double value) {
        this.value = value;
    }

    public String toString() {
        return "RegionEconomyProjection{id = " + id + ", regionName = " + regionName + ", scenarioName = " + scenarioName + ", value = " + value + "}";
    }

}

