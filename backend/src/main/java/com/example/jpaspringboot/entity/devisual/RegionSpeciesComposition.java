package com.example.jpaspringboot.entity.devisual;
import jakarta.persistence.*;

@Entity
@Table(
        name = "region_species_composition",
        uniqueConstraints = @UniqueConstraint(columnNames = {"region_name", "species_name"})
)
public class RegionSpeciesComposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "species_name")
    private String speciesName;

    @Column(name = "carbon_ratio")
    private Double carbonRatio;

    public RegionSpeciesComposition() {
    }

    public RegionSpeciesComposition(Integer id, String regionName, String speciesName, Double carbonRatio) {
        this.id = id;
        this.regionName = regionName;
        this.speciesName = speciesName;
        this.carbonRatio = carbonRatio;
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
     * @return speciesName
     */
    public String getSpeciesName() {
        return speciesName;
    }

    /**
     * 设置
     * @param speciesName
     */
    public void setSpeciesName(String speciesName) {
        this.speciesName = speciesName;
    }

    /**
     * 获取
     * @return carbonRatio
     */
    public Double getCarbonRatio() {
        return carbonRatio;
    }

    /**
     * 设置
     * @param carbonRatio
     */
    public void setCarbonRatio(Double carbonRatio) {
        this.carbonRatio = carbonRatio;
    }

    public String toString() {
        return "RegionSpeciesComposition{id = " + id + ", regionName = " + regionName + ", speciesName = " + speciesName + ", carbonRatio = " + carbonRatio + "}";
    }

    // Getters & Setters
}

