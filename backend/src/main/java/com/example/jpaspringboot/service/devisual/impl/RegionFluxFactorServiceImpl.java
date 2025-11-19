package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionFluxFactor;
import com.example.jpaspringboot.repository.devisual.RegionFluxFactorRepository;
import com.example.jpaspringboot.service.devisual.RegionFluxFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionFluxFactorServiceImpl implements RegionFluxFactorService {

    @Autowired
    private RegionFluxFactorRepository repository;

    @Override
    public List<RegionFluxFactor> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}
