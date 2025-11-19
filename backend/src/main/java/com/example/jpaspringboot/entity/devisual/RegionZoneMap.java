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

    // Getters & Setters
}

