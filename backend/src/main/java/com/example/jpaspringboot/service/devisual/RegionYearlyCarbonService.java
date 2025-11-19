package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionYearlyCarbon;

import java.util.List;

public interface RegionYearlyCarbonService {
    List<RegionYearlyCarbon> getByRegionName(String regionName);
}
