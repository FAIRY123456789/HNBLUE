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

    public VisualIndicatorData(Long id, String regionName, String indicatorKey, Double value, String unit, String description, Integer year) {
        this.id = id;
        this.regionName = regionName;
        this.indicatorKey = indicatorKey;
        this.value = value;
        this.unit = unit;
        this.description = description;
        this.year = year;
    }

    public VisualIndicatorData() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
