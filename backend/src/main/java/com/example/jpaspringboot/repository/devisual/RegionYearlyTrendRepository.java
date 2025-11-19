package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionYearlyTrend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionYearlyTrendRepository extends JpaRepository<RegionYearlyTrend, Integer> {
    List<RegionYearlyTrend> findByRegionName(String regionName);
}
