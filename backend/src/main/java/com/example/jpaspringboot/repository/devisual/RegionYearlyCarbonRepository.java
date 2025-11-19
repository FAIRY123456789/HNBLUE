package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionYearlyCarbon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionYearlyCarbonRepository extends JpaRepository<RegionYearlyCarbon, Integer> {
    List<RegionYearlyCarbon> findByRegionName(String regionName);
}
