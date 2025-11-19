package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionSpeciesComposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionSpeciesCompositionRepository extends JpaRepository<RegionSpeciesComposition, Integer> {
    List<RegionSpeciesComposition> findByRegionName(String regionName);
}
