package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionYearlyTrend;
import com.example.jpaspringboot.repository.devisual.RegionYearlyTrendRepository;
import com.example.jpaspringboot.service.devisual.RegionYearlyTrendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionYearlyTrendServiceImpl implements RegionYearlyTrendService {

    @Autowired
    private RegionYearlyTrendRepository repository;

    @Override
    public List<RegionYearlyTrend> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}
