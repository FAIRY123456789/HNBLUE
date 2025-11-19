/**
 * 区域通量因子实体类
 *
 * 功能概述：
 * • 映射区域环境因子与碳通量关系的实体对象
 * • 存储不同区域的气候环境参数及其对应的碳通量值
 * • 为碳通量预测模型提供基础数据支持
 *
 * 实体字段说明：
 * • regionName - 区域名称标识
 * • temp - 温度参数(℃)
 * • rainfall - 降雨量参数(mm)
 * • salinity - 盐度参数(‰)
 * • age - 林龄参数(年)
 * • flux - 碳通量测量值
 *
 * 应用场景：
 * • 区域碳通量统计分析
 * • 环境因子与碳通量关联分析
 * • 碳通量预测模型训练数据
 *
 * 数据库映射：
 * • 对应表名：region_flux_factors
 * • 主键策略：自增ID
 */
package com.example.jpaspringboot.entity.devisual;
import jakarta.persistence.*;
@Entity
@Table(name = "region_flux_factors")
public class RegionFluxFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String regionName;
    private Double temp;
    private Integer rainfall;
    private Double salinity;
    private Integer age;
    private Double flux;

    public RegionFluxFactor() {
    }

    /**
     * 全参数构造函数
     * @param id 主键ID
     * @param regionName 区域名称
     * @param temp 温度
     * @param rainfall 降雨量
     * @param salinity 盐度
     * @param age 林龄
     * @param flux 碳通量
     */
    public RegionFluxFactor(Integer id, String regionName, Double temp, Integer rainfall, Double salinity, Integer age, Double flux) {
        this.id = id;
        this.regionName = regionName;
        this.temp = temp;
        this.rainfall = rainfall;
        this.salinity = salinity;
        this.age = age;
        this.flux = flux;
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
     * @return temp
     */
    public Double getTemp() {
        return temp;
    }

    /**
     * 设置
     * @param temp
     */
    public void setTemp(Double temp) {
        this.temp = temp;
    }

    /**
     * 获取
     * @return rainfall
     */
    public Integer getRainfall() {
        return rainfall;
    }

    /**
     * 设置
     * @param rainfall
     */
    public void setRainfall(Integer rainfall) {
        this.rainfall = rainfall;
    }

    /**
     * 获取
     * @return salinity
     */
    public Double getSalinity() {
        return salinity;
    }

    /**
     * 设置
     * @param salinity
     */
    public void setSalinity(Double salinity) {
        this.salinity = salinity;
    }

    /**
     * 获取
     * @return age
     */
    public Integer getAge() {
        return age;
    }

    /**
     * 设置
     * @param age
     */
    public void setAge(Integer age) {
        this.age = age;
    }

    /**
     * 获取
     * @return flux
     */
    public Double getFlux() {
        return flux;
    }

    /**
     * 设置
     * @param flux
     */
    public void setFlux(Double flux) {
        this.flux = flux;
    }

    public String toString() {
        return "RegionFluxFactor{id = " + id + ", regionName = " + regionName + ", temp = " + temp + ", rainfall = " + rainfall + ", salinity = " + salinity + ", age = " + age + ", flux = " + flux + "}";
    }

    // Getters & Setters
}

