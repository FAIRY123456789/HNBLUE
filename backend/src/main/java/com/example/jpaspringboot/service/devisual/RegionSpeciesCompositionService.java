package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionSpeciesComposition;

import java.util.List;

public interface RegionSpeciesCompositionService {
    List<RegionSpeciesComposition> getByRegionName(String regionName);
}
