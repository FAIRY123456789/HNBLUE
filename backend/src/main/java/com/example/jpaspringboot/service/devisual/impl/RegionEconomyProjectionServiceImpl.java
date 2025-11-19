package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionEconomyProjection;
import com.example.jpaspringboot.repository.devisual.RegionEconomyProjectionRepository;
import com.example.jpaspringboot.service.devisual.RegionEconomyProjectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionEconomyProjectionServiceImpl implements RegionEconomyProjectionService {

    @Autowired
    private RegionEconomyProjectionRepository repository;

    @Override
    public List<RegionEconomyProjection> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}
