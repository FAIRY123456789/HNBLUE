package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionEconomyProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionEconomyProjectionRepository extends JpaRepository<RegionEconomyProjection, Integer> {
    List<RegionEconomyProjection> findByRegionName(String regionName);
}
