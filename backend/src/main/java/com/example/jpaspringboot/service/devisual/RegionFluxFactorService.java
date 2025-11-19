package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionFluxFactor;

import java.util.List;

public interface RegionFluxFactorService {
    List<RegionFluxFactor> getByRegionName(String regionName);
}
