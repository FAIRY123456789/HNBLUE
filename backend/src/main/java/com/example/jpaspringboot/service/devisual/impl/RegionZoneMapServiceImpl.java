package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionZoneMap;
import com.example.jpaspringboot.repository.devisual.RegionZoneMapRepository;
import com.example.jpaspringboot.service.devisual.RegionZoneMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegionZoneMapServiceImpl implements RegionZoneMapService {

    @Autowired
    private RegionZoneMapRepository repository;

    @Override
    public RegionZoneMap getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}
