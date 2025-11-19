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

