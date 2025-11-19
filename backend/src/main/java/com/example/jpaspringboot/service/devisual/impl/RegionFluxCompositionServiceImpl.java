package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionFluxComposition;
import com.example.jpaspringboot.repository.devisual.RegionFluxCompositionRepository;
import com.example.jpaspringboot.service.devisual.RegionFluxCompositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionFluxCompositionServiceImpl implements RegionFluxCompositionService {

    @Autowired
    private RegionFluxCompositionRepository repository;

    @Override
    public List<RegionFluxComposition> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}
