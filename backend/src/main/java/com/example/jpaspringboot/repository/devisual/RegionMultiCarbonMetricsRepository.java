package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionMultiCarbonMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionMultiCarbonMetricsRepository extends JpaRepository<RegionMultiCarbonMetrics, Integer> {
    RegionMultiCarbonMetrics findByRegionName(String regionName);
}
