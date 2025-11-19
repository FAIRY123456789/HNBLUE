package com.example.jpaspringboot.service.devisual.impl;

import com.example.jpaspringboot.entity.devisual.RegionSpeciesComposition;
import com.example.jpaspringboot.repository.devisual.RegionSpeciesCompositionRepository;
import com.example.jpaspringboot.service.devisual.RegionSpeciesCompositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionSpeciesCompositionServiceImpl implements RegionSpeciesCompositionService {

    @Autowired
    private RegionSpeciesCompositionRepository repository;

    @Override
    public List<RegionSpeciesComposition> getByRegionName(String regionName) {
        return repository.findByRegionName(regionName);
    }
}
