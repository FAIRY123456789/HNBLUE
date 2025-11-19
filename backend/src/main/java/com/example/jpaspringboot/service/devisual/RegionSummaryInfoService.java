package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionSummaryInfo;

public interface RegionSummaryInfoService {
    RegionSummaryInfo getByRegionName(String regionName);
}
