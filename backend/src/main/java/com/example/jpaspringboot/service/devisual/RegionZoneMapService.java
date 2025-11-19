package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionZoneMap;

public interface RegionZoneMapService {
    RegionZoneMap getByRegionName(String regionName);
}
