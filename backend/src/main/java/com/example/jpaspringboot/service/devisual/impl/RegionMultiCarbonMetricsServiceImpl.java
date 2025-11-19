package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionMultiCarbonMetrics;
import com.example.jpaspringboot.repository.devisual.RegionMultiCarbonMetricsRepository;
import com.example.jpaspringboot.service.devisual.RegionMultiCarbonMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegionMultiCarbonMetricsServiceImpl implements RegionMultiCarbonMetricsService {

    @Autowired
    private RegionMultiCarbonMetricsRepository repository;

    @Override
    public RegionMultiCarbonMetrics getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}
