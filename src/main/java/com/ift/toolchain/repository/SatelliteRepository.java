package com.ift.toolchain.repository;

import com.ift.toolchain.model.Satellite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SatelliteRepository extends JpaRepository<Satellite, String> {

    public Satellite findSatelliteByName(String satelliteName);

    public List<Satellite> findAllByOrbitName(String orbitName);
}
