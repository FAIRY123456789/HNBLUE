package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionSummaryInfo;
import com.example.jpaspringboot.repository.devisual.RegionSummaryInfoRepository;
import com.example.jpaspringboot.service.devisual.RegionSummaryInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegionSummaryInfoServiceImpl implements RegionSummaryInfoService {

    @Autowired
    private RegionSummaryInfoRepository repository;

    @Override
    public RegionSummaryInfo getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}
