package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionYearlyCarbon;
import com.example.jpaspringboot.repository.devisual.RegionYearlyCarbonRepository;
import com.example.jpaspringboot.service.devisual.RegionYearlyCarbonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionYearlyCarbonServiceImpl implements RegionYearlyCarbonService {

    @Autowired
    private RegionYearlyCarbonRepository repository;

    @Override
    public List<RegionYearlyCarbon> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}
