package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionZoneMap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionZoneMapRepository extends JpaRepository<RegionZoneMap, String> {
    RegionZoneMap findByRegionName(String regionName);
}
