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

    // Getters & Setters
}

