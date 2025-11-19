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

    // Getters & Setters
}

