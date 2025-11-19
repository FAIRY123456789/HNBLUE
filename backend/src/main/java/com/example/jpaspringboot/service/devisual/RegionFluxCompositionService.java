package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionFluxComposition;

import java.util.List;

public interface RegionFluxCompositionService {
    List<RegionFluxComposition> getByRegionName(String regionName);
}
