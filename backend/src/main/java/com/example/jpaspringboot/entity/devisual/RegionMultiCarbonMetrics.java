package com.example.jpaspringboot.entity.devisual;
import jakarta.persistence.*;
@Entity
@Table(name = "region_multi_carbon_metrics")
public class RegionMultiCarbonMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String regionName;
    private Double litterfall;
    private Double deadwood;
    private Double aerialRoot;
    private Double soil;
    private Double aboveground;

    public RegionMultiCarbonMetrics() {
    }

    public RegionMultiCarbonMetrics(Integer id, String regionName, Double litterfall, Double deadwood, Double aerialRoot, Double soil, Double aboveground) {
        this.id = id;
        this.regionName = regionName;
        this.litterfall = litterfall;
        this.deadwood = deadwood;
        this.aerialRoot = aerialRoot;
        this.soil = soil;
        this.aboveground = aboveground;
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
     * @return litterfall
     */
    public Double getLitterfall() {
        return litterfall;
    }

    /**
     * 设置
     * @param litterfall
     */
    public void setLitterfall(Double litterfall) {
        this.litterfall = litterfall;
    }

    /**
     * 获取
     * @return deadwood
     */
    public Double getDeadwood() {
        return deadwood;
    }

    /**
     * 设置
     * @param deadwood
     */
    public void setDeadwood(Double deadwood) {
        this.deadwood = deadwood;
    }

    /**
     * 获取
     * @return aerialRoot
     */
    public Double getAerialRoot() {
        return aerialRoot;
    }

    /**
     * 设置
     * @param aerialRoot
     */
    public void setAerialRoot(Double aerialRoot) {
        this.aerialRoot = aerialRoot;
    }

    /**
     * 获取
     * @return soil
     */
    public Double getSoil() {
        return soil;
    }

    /**
     * 设置
     * @param soil
     */
    public void setSoil(Double soil) {
        this.soil = soil;
    }

    /**
     * 获取
     * @return aboveground
     */
    public Double getAboveground() {
        return aboveground;
    }

    /**
     * 设置
     * @param aboveground
     */
    public void setAboveground(Double aboveground) {
        this.aboveground = aboveground;
    }

    public String toString() {
        return "RegionMultiCarbonMetrics{id = " + id + ", regionName = " + regionName + ", litterfall = " + litterfall + ", deadwood = " + deadwood + ", aerialRoot = " + aerialRoot + ", soil = " + soil + ", aboveground = " + aboveground + "}";
    }

    // Getters & Setters
}

