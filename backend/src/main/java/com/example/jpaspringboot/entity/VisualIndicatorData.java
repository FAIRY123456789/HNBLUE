/**
 * 可视化指标数据实体类
 *
 * 功能概述：
 * • 映射数据库可视化指标数据表结构
 * • 存储区域生态监测指标的时间序列数据
 * • 提供数据创建和更新的时间戳自动管理
 *
 * 数据字段说明：
 * • 区域名称、指标键值、数值、单位、描述信息
 * • 数据年份标识和时间戳记录
 * • 自增主键确保数据唯一性
 *
 * 业务用途：
 * • 生态监测数据持久化存储
 * • 区域环境指标统计分析
 * • 可视化图表数据源支持
 * • 历史数据趋势分析
 *
 * 技术特性：
 * • JPA注解实现对象关系映射
 * • Lombok简化Getter/Setter代码
 * • 生命周期回调自动维护时间戳
 * • 支持时序数据管理和查询
 */
package com.example.jpaspringboot.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "visual_indicator_data")
public class VisualIndicatorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "indicator_key")
    private String indicatorKey;

    private Double value;

    private String unit;

    private String description;

    private Integer year;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 全参构造方法 - 用于数据初始化和测试
     */
    public VisualIndicatorData(Long id, String regionName, String indicatorKey, Double value, String unit, String description, Integer year) {
        this.id = id;
        this.regionName = regionName;
        this.indicatorKey = indicatorKey;
        this.value = value;
        this.unit = unit;
        this.description = description;
        this.year = year;
    }
    /**
     * 无参构造方法 - JPA规范要求
     */
    public VisualIndicatorData() {
    }

    public VisualIndicatorData(Long id, String regionName, String indicatorKey, Double value, String unit, String description, Integer year, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.regionName = regionName;
        this.indicatorKey = indicatorKey;
        this.value = value;
        this.unit = unit;
        this.description = description;
        this.year = year;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 持久化前回调 - 自动设置创建和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    /**
     * 更新前回调 - 自动更新修改时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    /**
     * 获取
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(Long id) {
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
     * @return indicatorKey
     */
    public String getIndicatorKey() {
        return indicatorKey;
    }

    /**
     * 设置
     * @param indicatorKey
     */
    public void setIndicatorKey(String indicatorKey) {
        this.indicatorKey = indicatorKey;
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

    /**
     * 获取
     * @return unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * 设置
     * @param unit
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * 获取
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
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
     * @return createdAt
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置
     * @param createdAt
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取
     * @return updatedAt
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置
     * @param updatedAt
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String toString() {
        return "VisualIndicatorData{id = " + id + ", regionName = " + regionName + ", indicatorKey = " + indicatorKey + ", value = " + value + ", unit = " + unit + ", description = " + description + ", year = " + year + ", createdAt = " + createdAt + ", updatedAt = " + updatedAt + "}";
    }
}
