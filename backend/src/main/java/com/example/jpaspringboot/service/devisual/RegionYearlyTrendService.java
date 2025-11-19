package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionYearlyTrend;

import java.util.List;

public interface RegionYearlyTrendService {
    List<RegionYearlyTrend> getByRegionName(String regionName);
}
