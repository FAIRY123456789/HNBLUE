/**
 * 区域分区映射实体类
 *
 * 功能概述：
 * • 存储和管理生态保护区域的空间分区数据
 * • 以JSON格式记录不同功能区的空间边界信息
 * • 支持生态保护区的多层级分区管理
 *
 * 数据库映射：
 * • 表名：region_zone_map
 * • 主键：region_name（区域名称）
 * • JSON字段存储空间几何数据或分区配置
 *
 * 分区类型：
 * • 核心区 (coreZone) - 严格保护的核心生态区域
 * • 缓冲区 (bufferZone) - 核心区外围的缓冲地带
 * • 潮间带 (intertidalZone) - 海岸潮汐影响区域
 * • 风险区 (riskZone) - 生态风险评估区域
 *
 * 应用场景：
 * • 生态保护区划管理
 * • 空间规划数据存储
 * • 环境监测区域定义
 * • 风险评估数据支撑
 */
package com.example.jpaspringboot.entity.devisual;
import jakarta.persistence.*;
@Entity
@Table(name = "region_zone_map")
public class RegionZoneMap {

    @Id
    private String regionName;

    @Lob
    @Column(columnDefinition = "JSON")
    private String coreZone;

    @Lob
    @Column(columnDefinition = "JSON")
    private String bufferZone;

    @Lob
    @Column(columnDefinition = "JSON")
    private String intertidalZone;

    @Lob
    @Column(columnDefinition = "JSON")
    private String riskZone;

    public RegionZoneMap() {
    }

    public RegionZoneMap(String regionName, String coreZone, String bufferZone, String intertidalZone, String riskZone) {
        this.regionName = regionName;
        this.coreZone = coreZone;
        this.bufferZone = bufferZone;
        this.intertidalZone = intertidalZone;
        this.riskZone = riskZone;
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
     * @return coreZone
     */
    public String getCoreZone() {
        return coreZone;
    }

    /**
     * 设置
     * @param coreZone
     */
    public void setCoreZone(String coreZone) {
        this.coreZone = coreZone;
    }

    /**
     * 获取
     * @return bufferZone
     */
    public String getBufferZone() {
        return bufferZone;
    }

    /**
     * 设置
     * @param bufferZone
     */
    public void setBufferZone(String bufferZone) {
        this.bufferZone = bufferZone;
    }

    /**
     * 获取
     * @return intertidalZone
     */
    public String getIntertidalZone() {
        return intertidalZone;
    }

    /**
     * 设置
     * @param intertidalZone
     */
    public void setIntertidalZone(String intertidalZone) {
        this.intertidalZone = intertidalZone;
    }

    /**
     * 获取
     * @return riskZone
     */
    public String getRiskZone() {
        return riskZone;
    }

    /**
     * 设置
     * @param riskZone
     */
    public void setRiskZone(String riskZone) {
        this.riskZone = riskZone;
    }

    public String toString() {
        return "RegionZoneMap{regionName = " + regionName + ", coreZone = " + coreZone + ", bufferZone = " + bufferZone + ", intertidalZone = " + intertidalZone + ", riskZone = " + riskZone + "}";
    }

}

