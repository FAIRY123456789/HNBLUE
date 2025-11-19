package com.example.jpaspringboot.entity.devisual;
import jakarta.persistence.*;
@Entity
@Table(
        name = "region_flux_composition",
        uniqueConstraints = @UniqueConstraint(columnNames = {"region_name", "year"}) // ✅ 用数据库字段名
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

    // Getters & Setters
}
