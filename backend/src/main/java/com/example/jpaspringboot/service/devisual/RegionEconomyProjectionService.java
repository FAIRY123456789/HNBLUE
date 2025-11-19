package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionEconomyProjection;

import java.util.List;

public interface RegionEconomyProjectionService {
    List<RegionEconomyProjection> getByRegionName(String regionName);
}
